package com.platform.talent.jobposting.service;

import com.platform.talent.jobposting.domain.model.JobPosting;
import com.platform.talent.jobposting.domain.model.JobPostingStatus;
import com.platform.talent.jobposting.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JobBoardPublisher jobBoardPublisher;

    @KafkaListener(topics = "talent.requisition.approved", groupId = "job-posting-service")
    public void handleRequisitionApproved(Map<String, Object> event) {
        log.info("Creating job posting from approved requisition");
        
        String requisitionId = (String) event.get("requisitionId");
        String organizationId = (String) event.get("organizationId");
        
        JobPosting posting = JobPosting.builder()
            .jobId(UUID.randomUUID().toString())
            .organizationId(organizationId)
            .requisitionId(requisitionId)
            .jobTitle((String) event.get("jobTitle"))
            .jobDescription((String) event.get("jobDescription"))
            .department((String) event.get("department"))
            .location((String) event.get("location"))
            .workType((String) event.get("workType"))
            .employmentType((String) event.get("employmentType"))
            .status(JobPostingStatus.DRAFT)
            .viewCount(0)
            .applicationCount(0)
            .createdDate(LocalDate.now())
            .createdBy("system")
            .build();
        
        jobPostingRepository.save(posting);
        log.info("Job posting created from requisition: {}", requisitionId);
    }

    @Transactional
    public JobPosting createJobPosting(JobPosting posting, String userId) {
        posting.setJobId(UUID.randomUUID().toString());
        posting.setStatus(JobPostingStatus.DRAFT);
        posting.setViewCount(0);
        posting.setApplicationCount(0);
        posting.setCreatedDate(LocalDate.now());
        posting.setCreatedBy(userId);

        return jobPostingRepository.save(posting);
    }

    @Transactional
    public JobPosting publishJob(String organizationId, String jobId, String userId) {
        JobPosting posting = getJobPosting(organizationId, jobId);

        posting.setStatus(JobPostingStatus.PUBLISHED);
        posting.setPublishedDate(LocalDate.now());
        posting.setLastModifiedDate(LocalDate.now());

        // Publish to external job boards
        if (Boolean.TRUE.equals(posting.getPublishToLinkedIn())) {
            String linkedInId = jobBoardPublisher.publishToLinkedIn(posting);
            posting.setLinkedInJobId(linkedInId);
        }
        if (Boolean.TRUE.equals(posting.getPublishToIndeed())) {
            String indeedId = jobBoardPublisher.publishToIndeed(posting);
            posting.setIndeedJobId(indeedId);
        }

        JobPosting saved = jobPostingRepository.save(posting);

        kafkaTemplate.send("talent.job.posted", Map.of(
            "jobId", saved.getJobId(),
            "requisitionId", saved.getRequisitionId(),
            "organizationId", organizationId
        ));

        return saved;
    }

    @Transactional
    public JobPosting pauseJob(String organizationId, String jobId) {
        JobPosting posting = getJobPosting(organizationId, jobId);
        posting.setStatus(JobPostingStatus.PAUSED);
        posting.setLastModifiedDate(LocalDate.now());
        return jobPostingRepository.save(posting);
    }

    @Transactional
    public JobPosting closeJob(String organizationId, String jobId) {
        JobPosting posting = getJobPosting(organizationId, jobId);
        posting.setStatus(JobPostingStatus.CLOSED);
        posting.setLastModifiedDate(LocalDate.now());

        kafkaTemplate.send("talent.job.closed", Map.of(
            "jobId", jobId,
            "organizationId", organizationId
        ));

        return jobPostingRepository.save(posting);
    }

    @Transactional
    public void incrementViewCount(String jobId) {
        jobPostingRepository.findById(jobId).ifPresent(job -> {
            job.setViewCount(job.getViewCount() + 1);
            jobPostingRepository.save(job);
        });
    }

    @Transactional
    public void incrementApplicationCount(String jobId) {
        jobPostingRepository.findById(jobId).ifPresent(job -> {
            job.setApplicationCount(job.getApplicationCount() + 1);
            jobPostingRepository.save(job);
        });
    }

    @Transactional(readOnly = true)
    public JobPosting getJobPosting(String organizationId, String jobId) {
        return jobPostingRepository.findById(jobId)
            .filter(j -> j.getOrganizationId().equals(organizationId))
            .orElseThrow(() -> new RuntimeException("Job posting not found"));
    }

    @Transactional(readOnly = true)
    public List<JobPosting> getAllJobPostings(String organizationId) {
        return jobPostingRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public List<JobPosting> getPublishedJobs() {
        return jobPostingRepository.findAllPublished();
    }

    @Transactional(readOnly = true)
    public List<JobPosting> searchJobs(String keyword) {
        return jobPostingRepository.searchJobs(keyword);
    }

    @Transactional
    public JobPosting updateJobPosting(String organizationId, String jobId, JobPosting updates, String userId) {
        JobPosting existing = getJobPosting(organizationId, jobId);

        if (updates.getJobTitle() != null) existing.setJobTitle(updates.getJobTitle());
        if (updates.getJobDescription() != null) existing.setJobDescription(updates.getJobDescription());
        if (updates.getResponsibilities() != null) existing.setResponsibilities(updates.getResponsibilities());
        if (updates.getQualifications() != null) existing.setQualifications(updates.getQualifications());
        if (updates.getBenefits() != null) existing.setBenefits(updates.getBenefits());
        if (updates.getSalaryMin() != null) existing.setSalaryMin(updates.getSalaryMin());
        if (updates.getSalaryMax() != null) existing.setSalaryMax(updates.getSalaryMax());
        if (updates.getShowSalary() != null) existing.setShowSalary(updates.getShowSalary());
        if (updates.getExpiryDate() != null) existing.setExpiryDate(updates.getExpiryDate());
        if (updates.getPublishToLinkedIn() != null) existing.setPublishToLinkedIn(updates.getPublishToLinkedIn());
        if (updates.getPublishToIndeed() != null) existing.setPublishToIndeed(updates.getPublishToIndeed());

        existing.setLastModifiedDate(LocalDate.now());

        return jobPostingRepository.save(existing);
    }
}

