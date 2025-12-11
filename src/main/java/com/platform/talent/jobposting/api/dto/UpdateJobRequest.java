package com.platform.talent.jobposting.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
public class UpdateJobRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    private String employmentType;

    private String experienceLevel;

    @Min(value = 0, message = "Minimum salary must be positive")
    private Double salaryMin;

    @Min(value = 0, message = "Maximum salary must be positive")
    private Double salaryMax;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String salaryCurrency;

    private UUID departmentId;

    private UUID hiringManagerId;

    @Min(value = 1, message = "Number of positions must be at least 1")
    private Integer numberOfPositions;

    private LocalDateTime expiresAt;

    private Map<String, Object> customFields;

    private Map<String, Object> requirements;

    private Map<String, Object> benefits;

    private Boolean isRemote;

    private Boolean isFeatured;
}

