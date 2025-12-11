package com.platform.talent.jobposting.api.dto;

import com.platform.talent.jobposting.domain.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchCriteria {

    private UUID tenantId;
    private String keyword;
    private JobStatus status;
    private String location;
    private String employmentType;
    private String experienceLevel;
    private UUID departmentId;
    private UUID recruiterId;
    private Boolean isRemote;
    private Boolean activeOnly;
}

