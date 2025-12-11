package com.platform.talent.jobposting.service;

import com.platform.talent.jobposting.api.dto.CreateJobRequest;
import com.platform.talent.jobposting.api.dto.JobResponse;
import com.platform.talent.jobposting.api.dto.JobSearchCriteria;
import com.platform.talent.jobposting.api.dto.UpdateJobRequest;
import com.platform.talent.jobposting.domain.model.Job;
import com.platform.talent.jobposting.domain.model.JobStatus;
import com.platform.talent.jobposting.domain.repository.JobRepository;
import com.platform.talent.jobposting.service.integration.KernelIntegrationService;
import com.platform.talent.jobposting.service.integration.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final KernelIntegrationService kernelService;
    private final EmailNotificationService emailService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public JobResponse createJob(UUID tenantId, CreateJobRequest request) {
        log.info("Creating job for tenant: {}", tenantId);

        Job job = Job.builder()
                .tenantId(tenantId)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .employmentType(request.getEmploymentType())
                .experienceLevel(request.getExperienceLevel())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency())
                .status(JobStatus.DRAFT)
                .departmentId(request.getDepartmentId())
                .recruiterId(request.getRecruiterId())
                .hiringManagerId(request.getHiringManagerId())
                .numberOfPositions(request.getNumberOfPositions())
                .expiresAt(request.getExpiresAt())
                .customFields(request.getCustomFields())
                .requirements(request.getRequirements())
                .benefits(request.getBenefits())
                .isRemote(request.getIsRemote())
                .isFeatured(request.getIsFeatured())
                .applicationCount(0)
                .viewCount(0)
                .build();

        job = jobRepository.save(job);

        // Store custom fields in Kernel if present
        if (request.getCustomFields() != null && !request.getCustomFields().isEmpty()) {
            kernelService.storeExtendedAttributes(job.getId(), "Job", request.getCustomFields());
        }

        log.info("Job created successfully: {}", job.getId());
        return mapToResponse(job);
    }

    @Transactional
    public JobResponse updateJob(UUID tenantId, UUID jobId, UpdateJobRequest request) {
        log.info("Updating job: {} for tenant: {}", jobId, tenantId);

        Job job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.canBeEdited()) {
            throw new RuntimeException("Job cannot be edited in current status: " + job.getStatus());
        }

        // Update fields if provided
        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getLocation() != null) job.setLocation(request.getLocation());
        if (request.getEmploymentType() != null) job.setEmploymentType(request.getEmploymentType());
        if (request.getExperienceLevel() != null) job.setExperienceLevel(request.getExperienceLevel());
        if (request.getSalaryMin() != null) job.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) job.setSalaryMax(request.getSalaryMax());
        if (request.getSalaryCurrency() != null) job.setSalaryCurrency(request.getSalaryCurrency());
        if (request.getDepartmentId() != null) job.setDepartmentId(request.getDepartmentId());
        if (request.getHiringManagerId() != null) job.setHiringManagerId(request.getHiringManagerId());
        if (request.getNumberOfPositions() != null) job.setNumberOfPositions(request.getNumberOfPositions());
        if (request.getExpiresAt() != null) job.setExpiresAt(request.getExpiresAt());
        if (request.getCustomFields() != null) job.setCustomFields(request.getCustomFields());
        if (request.getRequirements() != null) job.setRequirements(request.getRequirements());
        if (request.getBenefits() != null) job.setBenefits(request.getBenefits());
        if (request.getIsRemote() != null) job.setIsRemote(request.getIsRemote());
        if (request.getIsFeatured() != null) job.setIsFeatured(request.getIsFeatured());

        job = jobRepository.save(job);

        log.info("Job updated successfully: {}", jobId);
        return mapToResponse(job);
    }

    @Transactional
    public void publishJob(UUID tenantId, UUID jobId) {
        log.info("Publishing job: {} for tenant: {}", jobId, tenantId);

        Job job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.canBePublished() && job.getStatus() != JobStatus.DRAFT) {
            throw new RuntimeException("Job cannot be published in current status: " + job.getStatus());
        }

        job.setStatus(JobStatus.PUBLISHED);
        job.setPublishedAt(LocalDateTime.now());

        jobRepository.save(job);

        // Publish Kafka event
        publishJobEvent("job.published", job);

        // Send notification email
        emailService.sendJobPublishedNotification(job);

        log.info("Job published successfully: {}", jobId);
    }

    @Transactional
    public void closeJob(UUID tenantId, UUID jobId) {
        log.info("Closing job: {} for tenant: {}", jobId, tenantId);

        Job job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);

        // Publish Kafka event
        publishJobEvent("job.closed", job);

        log.info("Job closed successfully: {}", jobId);
    }

    @Transactional
    public void approveJob(UUID tenantId, UUID jobId, UUID approverId) {
        log.info("Approving job: {} for tenant: {} by approver: {}", jobId, tenantId, approverId);

        Job job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.getStatus() != JobStatus.PENDING_APPROVAL && job.getStatus() != JobStatus.DRAFT) {
            throw new RuntimeException("Job cannot be approved in current status: " + job.getStatus());
        }

        job.setStatus(JobStatus.APPROVED);
        job.setApprovedAt(LocalDateTime.now());
        job.setApprovedBy(approverId);

        jobRepository.save(job);

        // Publish Kafka event
        publishJobEvent("job.approved", job);

        // Send notification email
        emailService.sendJobApprovedNotification(job);

        log.info("Job approved successfully: {}", jobId);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID tenantId, UUID jobId) {
        Job job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Increment view count
        job.incrementViewCount();
        jobRepository.save(job);

        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> listJobs(UUID tenantId, JobStatus status, Pageable pageable) {
        Page<Job> jobs;
        if (status != null) {
            jobs = jobRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            jobs = jobRepository.findByTenantId(tenantId, pageable);
        }
        return jobs.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobs(JobSearchCriteria criteria, Pageable pageable) {
        Page<Job> jobs;

        if (criteria.getActiveOnly() != null && criteria.getActiveOnly()) {
            jobs = jobRepository.findActiveJobs(criteria.getTenantId(), LocalDateTime.now(), pageable);
        } else if (criteria.getKeyword() != null) {
            jobs = jobRepository.searchJobs(criteria.getTenantId(), criteria.getKeyword(), pageable);
        } else if (criteria.getRecruiterId() != null) {
            jobs = jobRepository.findByRecruiter(criteria.getTenantId(), criteria.getRecruiterId(), pageable);
        } else {
            jobs = jobRepository.findByTenantId(criteria.getTenantId(), pageable);
        }

        return jobs.map(this::mapToResponse);
    }

    @Transactional
    public void deleteJob(UUID tenantId, UUID jobId) {
        log.info("Deleting job: {} for tenant: {}", jobId, tenantId);

        Job job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.getStatus() == JobStatus.PUBLISHED) {
            throw new RuntimeException("Cannot delete a published job. Close it first.");
        }

        jobRepository.delete(job);
        log.info("Job deleted successfully: {}", jobId);
    }

    private void publishJobEvent(String eventType, Job job) {
        try {
            kafkaTemplate.send("talent.job.events", job.getId().toString(), Map.of(
                "eventType", eventType,
                "jobId", job.getId(),
                "tenantId", job.getTenantId(),
                "status", job.getStatus(),
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Failed to publish job event: {}", eventType, e);
        }
    }

    private JobResponse mapToResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .title(job.getTitle())
                .description(job.getDescription())
                .location(job.getLocation())
                .employmentType(job.getEmploymentType())
                .experienceLevel(job.getExperienceLevel())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .status(job.getStatus())
                .departmentId(job.getDepartmentId())
                .recruiterId(job.getRecruiterId())
                .hiringManagerId(job.getHiringManagerId())
                .numberOfPositions(job.getNumberOfPositions())
                .publishedAt(job.getPublishedAt())
                .expiresAt(job.getExpiresAt())
                .approvedAt(job.getApprovedAt())
                .approvedBy(job.getApprovedBy())
                .customFields(job.getCustomFields())
                .requirements(job.getRequirements())
                .benefits(job.getBenefits())
                .applicationCount(job.getApplicationCount())
                .viewCount(job.getViewCount())
                .isRemote(job.getIsRemote())
                .isFeatured(job.getIsFeatured())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .createdBy(job.getCreatedBy())
                .updatedBy(job.getUpdatedBy())
                .build();
    }
}

