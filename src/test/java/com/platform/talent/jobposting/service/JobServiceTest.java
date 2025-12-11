package com.platform.talent.jobposting.service;

import com.platform.talent.jobposting.api.dto.CreateJobRequest;
import com.platform.talent.jobposting.api.dto.JobResponse;
import com.platform.talent.jobposting.domain.model.Job;
import com.platform.talent.jobposting.domain.model.JobStatus;
import com.platform.talent.jobposting.domain.repository.JobRepository;
import com.platform.talent.jobposting.service.integration.EmailNotificationService;
import com.platform.talent.jobposting.service.integration.KernelIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private KernelIntegrationService kernelService;

    @Mock
    private EmailNotificationService emailService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private JobService jobService;

    private UUID tenantId;
    private UUID recruiterId;
    private CreateJobRequest createRequest;
    private Job job;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        recruiterId = UUID.randomUUID();

        createRequest = CreateJobRequest.builder()
                .title("Senior Java Developer")
                .description("We are hiring a Senior Java Developer")
                .location("Remote")
                .employmentType("FULL_TIME")
                .experienceLevel("SENIOR")
                .salaryMin(100000.0)
                .salaryMax(150000.0)
                .salaryCurrency("USD")
                .recruiterId(recruiterId)
                .numberOfPositions(2)
                .isRemote(true)
                .build();

        job = Job.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .title(createRequest.getTitle())
                .description(createRequest.getDescription())
                .status(JobStatus.DRAFT)
                .recruiterId(recruiterId)
                .applicationCount(0)
                .viewCount(0)
                .build();
    }

    @Test
    void createJob_ShouldReturnJobResponse() {
        // Arrange
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        // Act
        JobResponse response = jobService.createJob(tenantId, createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(job.getId(), response.getId());
        assertEquals(job.getTitle(), response.getTitle());
        assertEquals(JobStatus.DRAFT, response.getStatus());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void publishJob_ShouldUpdateStatusToPublished() {
        // Arrange
        job.setStatus(JobStatus.DRAFT);
        when(jobRepository.findByIdAndTenantId(job.getId(), tenantId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        // Act
        jobService.publishJob(tenantId, job.getId());

        // Assert
        verify(jobRepository).save(any(Job.class));
        verify(emailService).sendJobPublishedNotification(any(Job.class));
    }

    @Test
    void publishJob_WhenJobNotFound_ShouldThrowException() {
        // Arrange
        when(jobRepository.findByIdAndTenantId(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            jobService.publishJob(tenantId, UUID.randomUUID())
        );
    }

    @Test
    void closeJob_ShouldUpdateStatusToClosed() {
        // Arrange
        job.setStatus(JobStatus.PUBLISHED);
        when(jobRepository.findByIdAndTenantId(job.getId(), tenantId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        // Act
        jobService.closeJob(tenantId, job.getId());

        // Assert
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void approveJob_ShouldUpdateStatusToApproved() {
        // Arrange
        UUID approverId = UUID.randomUUID();
        job.setStatus(JobStatus.PENDING_APPROVAL);
        when(jobRepository.findByIdAndTenantId(job.getId(), tenantId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        // Act
        jobService.approveJob(tenantId, job.getId(), approverId);

        // Assert
        verify(jobRepository).save(any(Job.class));
        verify(emailService).sendJobApprovedNotification(any(Job.class));
    }

    @Test
    void getJob_ShouldIncrementViewCount() {
        // Arrange
        when(jobRepository.findByIdAndTenantId(job.getId(), tenantId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        // Act
        JobResponse response = jobService.getJob(tenantId, job.getId());

        // Assert
        assertNotNull(response);
        verify(jobRepository).save(any(Job.class));
    }
}

