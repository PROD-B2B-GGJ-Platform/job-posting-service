package com.platform.talent.jobposting.api;

import com.platform.talent.jobposting.domain.model.JobPosting;
import com.platform.talent.jobposting.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;

    @PostMapping
    public ResponseEntity<JobPosting> createJobPosting(
        @RequestHeader("X-Organization-Id") String organizationId,
        @RequestHeader("X-User-Id") String userId,
        @RequestBody JobPosting posting
    ) {
        posting.setOrganizationId(organizationId);
        JobPosting created = jobPostingService.createJobPosting(posting, userId);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<List<JobPosting>> getAllJobPostings(
        @RequestHeader("X-Organization-Id") String organizationId
    ) {
        return ResponseEntity.ok(jobPostingService.getAllJobPostings(organizationId));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobPosting> getJobPosting(
        @RequestHeader("X-Organization-Id") String organizationId,
        @PathVariable String jobId
    ) {
        jobPostingService.incrementViewCount(jobId);
        return ResponseEntity.ok(jobPostingService.getJobPosting(organizationId, jobId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<JobPosting>> getPublishedJobs() {
        return ResponseEntity.ok(jobPostingService.getPublishedJobs());
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobPosting>> searchJobs(@RequestParam String keyword) {
        return ResponseEntity.ok(jobPostingService.searchJobs(keyword));
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<JobPosting> updateJobPosting(
        @RequestHeader("X-Organization-Id") String organizationId,
        @RequestHeader("X-User-Id") String userId,
        @PathVariable String jobId,
        @RequestBody JobPosting updates
    ) {
        return ResponseEntity.ok(jobPostingService.updateJobPosting(organizationId, jobId, updates, userId));
    }

    @PostMapping("/{jobId}/publish")
    public ResponseEntity<JobPosting> publishJob(
        @RequestHeader("X-Organization-Id") String organizationId,
        @RequestHeader("X-User-Id") String userId,
        @PathVariable String jobId
    ) {
        return ResponseEntity.ok(jobPostingService.publishJob(organizationId, jobId, userId));
    }

    @PostMapping("/{jobId}/pause")
    public ResponseEntity<JobPosting> pauseJob(
        @RequestHeader("X-Organization-Id") String organizationId,
        @PathVariable String jobId
    ) {
        return ResponseEntity.ok(jobPostingService.pauseJob(organizationId, jobId));
    }

    @PostMapping("/{jobId}/close")
    public ResponseEntity<JobPosting> closeJob(
        @RequestHeader("X-Organization-Id") String organizationId,
        @PathVariable String jobId
    ) {
        return ResponseEntity.ok(jobPostingService.closeJob(organizationId, jobId));
    }
}

