package com.platform.talent.jobposting.service;

import com.platform.talent.jobposting.domain.model.JobPosting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobBoardPublisher {

    private final RestTemplate restTemplate;

    public String publishToLinkedIn(JobPosting posting) {
        log.info("Publishing job to LinkedIn: {}", posting.getJobTitle());

        try {
            // LinkedIn Jobs API integration
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // headers.setBearerAuth(linkedInApiKey);

            Map<String, Object> jobData = Map.of(
                "title", posting.getJobTitle(),
                "description", posting.getJobDescription(),
                "location", posting.getLocation() != null ? posting.getLocation() : "Remote",
                "employmentType", mapEmploymentType(posting.getEmploymentType()),
                "company", posting.getOrganizationId()
            );

            // Simulated response - in production, this would call LinkedIn API
            String linkedInJobId = "LI-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Job published to LinkedIn with ID: {}", linkedInJobId);
            return linkedInJobId;

        } catch (Exception e) {
            log.error("Failed to publish to LinkedIn", e);
            return null;
        }
    }

    public String publishToIndeed(JobPosting posting) {
        log.info("Publishing job to Indeed: {}", posting.getJobTitle());

        try {
            // Indeed Jobs API integration
            Map<String, Object> jobData = Map.of(
                "jobTitle", posting.getJobTitle(),
                "jobDescription", posting.getJobDescription(),
                "location", posting.getLocation() != null ? posting.getLocation() : "Remote",
                "jobType", posting.getEmploymentType(),
                "salary", formatSalary(posting)
            );

            // Simulated response
            String indeedJobId = "IND-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Job published to Indeed with ID: {}", indeedJobId);
            return indeedJobId;

        } catch (Exception e) {
            log.error("Failed to publish to Indeed", e);
            return null;
        }
    }

    public String publishToGlassdoor(JobPosting posting) {
        log.info("Publishing job to Glassdoor: {}", posting.getJobTitle());

        try {
            String glassdoorJobId = "GD-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Job published to Glassdoor with ID: {}", glassdoorJobId);
            return glassdoorJobId;

        } catch (Exception e) {
            log.error("Failed to publish to Glassdoor", e);
            return null;
        }
    }

    public void unpublishFromLinkedIn(String linkedInJobId) {
        log.info("Unpublishing job from LinkedIn: {}", linkedInJobId);
        // LinkedIn API call to remove job
    }

    public void unpublishFromIndeed(String indeedJobId) {
        log.info("Unpublishing job from Indeed: {}", indeedJobId);
        // Indeed API call to remove job
    }

    private String mapEmploymentType(String type) {
        if (type == null) return "FULL_TIME";
        return switch (type.toUpperCase()) {
            case "FULL_TIME" -> "FULL_TIME";
            case "PART_TIME" -> "PART_TIME";
            case "CONTRACT" -> "CONTRACT";
            case "INTERNSHIP" -> "INTERNSHIP";
            default -> "OTHER";
        };
    }

    private String formatSalary(JobPosting posting) {
        if (posting.getSalaryMin() == null && posting.getSalaryMax() == null) {
            return "Competitive";
        }
        if (posting.getSalaryMin() != null && posting.getSalaryMax() != null) {
            return String.format("%s %s - %s %s", 
                posting.getCurrency(), posting.getSalaryMin(),
                posting.getCurrency(), posting.getSalaryMax());
        }
        return "Competitive";
    }
}

