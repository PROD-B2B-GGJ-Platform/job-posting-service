package com.platform.talent.jobposting.api.dto;

import com.platform.talent.jobposting.domain.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private UUID id;
    private UUID tenantId;
    private String title;
    private String description;
    private String location;
    private String employmentType;
    private String experienceLevel;
    private Double salaryMin;
    private Double salaryMax;
    private String salaryCurrency;
    private JobStatus status;
    private UUID departmentId;
    private UUID recruiterId;
    private UUID hiringManagerId;
    private Integer numberOfPositions;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime approvedAt;
    private UUID approvedBy;
    private Map<String, Object> customFields;
    private Map<String, Object> requirements;
    private Map<String, Object> benefits;
    private Integer applicationCount;
    private Integer viewCount;
    private Boolean isRemote;
    private Boolean isFeatured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}

