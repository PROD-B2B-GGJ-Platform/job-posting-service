package com.platform.talent.jobposting.api.controller;

import com.platform.talent.jobposting.api.dto.CreateJobRequest;
import com.platform.talent.jobposting.api.dto.JobResponse;
import com.platform.talent.jobposting.api.dto.JobSearchCriteria;
import com.platform.talent.jobposting.api.dto.UpdateJobRequest;
import com.platform.talent.jobposting.domain.model.JobStatus;
import com.platform.talent.jobposting.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Posting", description = "Job posting management API")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create a new job posting")
    public ResponseEntity<JobResponse> createJob(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.createJob(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<JobResponse> getJob(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id) {
        JobResponse response = jobService.getJob(tenantId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all jobs")
    public ResponseEntity<Page<JobResponse>> listJobs(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(required = false) JobStatus status,
            Pageable pageable) {
        Page<JobResponse> response = jobService.listJobs(tenantId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    @Operation(summary = "Search jobs")
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @RequestBody JobSearchCriteria criteria,
            Pageable pageable) {
        Page<JobResponse> response = jobService.searchJobs(criteria, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update job")
    public ResponseEntity<JobResponse> updateJob(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobRequest request) {
        JobResponse response = jobService.updateJob(tenantId, id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish job")
    public ResponseEntity<Void> publishJob(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id) {
        jobService.publishJob(tenantId, id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Close job")
    public ResponseEntity<Void> closeJob(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id) {
        jobService.closeJob(tenantId, id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve job")
    public ResponseEntity<Void> approveJob(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id,
            @RequestHeader("X-User-ID") UUID approverId) {
        jobService.approveJob(tenantId, id, approverId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete job")
    public ResponseEntity<Void> deleteJob(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id) {
        jobService.deleteJob(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "job-posting-service",
            "version", "10.0.0.1"
        ));
    }
}

