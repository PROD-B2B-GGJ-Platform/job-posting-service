package com.platform.talent.jobposting.service.integration;

import com.platform.talent.jobposting.domain.model.Job;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final RestTemplate restTemplate;

    @Value("${integration.email.url:http://business-email-service:8096}")
    private String emailServiceUrl;

    @CircuitBreaker(name = "email", fallbackMethod = "sendEmailFallback")
    @Retry(name = "email")
    public void sendJobPublishedNotification(Job job) {
        try {
            String url = emailServiceUrl + "/api/v1/email/send";
            Map<String, Object> request = Map.of(
                "to", new String[]{"recruiter@platform.com"}, // In real scenario, get from user service
                "subject", "Job Published: " + job.getTitle(),
                "template", "job-published",
                "data", Map.of(
                    "jobTitle", job.getTitle(),
                    "jobId", job.getId().toString()
                )
            );
            restTemplate.postForEntity(url, request, Void.class);
            log.info("Sent job published notification for job: {}", job.getId());
        } catch (Exception e) {
            log.error("Failed to send job published notification", e);
        }
    }

    @CircuitBreaker(name = "email", fallbackMethod = "sendEmailFallback")
    @Retry(name = "email")
    public void sendJobApprovedNotification(Job job) {
        try {
            String url = emailServiceUrl + "/api/v1/email/send";
            Map<String, Object> request = Map.of(
                "to", new String[]{"recruiter@platform.com"},
                "subject", "Job Approved: " + job.getTitle(),
                "template", "job-approved",
                "data", Map.of(
                    "jobTitle", job.getTitle(),
                    "jobId", job.getId().toString(),
                    "approvedAt", job.getApprovedAt().toString()
                )
            );
            restTemplate.postForEntity(url, request, Void.class);
            log.info("Sent job approved notification for job: {}", job.getId());
        } catch (Exception e) {
            log.error("Failed to send job approved notification", e);
        }
    }

    // Fallback methods
    public void sendEmailFallback(Job job, Exception e) {
        log.warn("Email service unavailable, notification not sent for job: {}", job.getId());
        // Could queue for later retry or log to database
    }
}

