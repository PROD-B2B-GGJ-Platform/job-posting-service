package com.platform.talent.jobposting.domain.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ggj_jobs", indexes = {
    @Index(name = "idx_job_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_job_published_at", columnList = "published_at"),
    @Index(name = "idx_job_expires_at", columnList = "expires_at"),
    @Index(name = "idx_job_recruiter", columnList = "recruiter_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String location;

    @Column(name = "employment_type", length = 50)
    private String employmentType; // FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP

    @Column(name = "experience_level", length = 50)
    private String experienceLevel; // ENTRY, MID, SENIOR, LEAD, EXECUTIVE

    @Column(name = "salary_min")
    private Double salaryMin;

    @Column(name = "salary_max")
    private Double salaryMax;

    @Column(name = "salary_currency", length = 3)
    private String salaryCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private JobStatus status;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "recruiter_id", nullable = false)
    private UUID recruiterId;

    @Column(name = "hiring_manager_id")
    private UUID hiringManagerId;

    @Column(name = "number_of_positions")
    private Integer numberOfPositions;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Type(JsonBinaryType.class)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private Map<String, Object> customFields;

    @Type(JsonBinaryType.class)
    @Column(name = "requirements", columnDefinition = "jsonb")
    private Map<String, Object> requirements;

    @Type(JsonBinaryType.class)
    @Column(name = "benefits", columnDefinition = "jsonb")
    private Map<String, Object> benefits;

    @Column(name = "application_count")
    @Builder.Default
    private Integer applicationCount = 0;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "is_remote")
    private Boolean isRemote;

    @Column(name = "is_featured")
    private Boolean isFeatured;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    private Long version;

    // Business methods
    public boolean canBePublished() {
        return status == JobStatus.APPROVED && publishedAt == null;
    }

    public boolean isActive() {
        return status == JobStatus.PUBLISHED 
            && expiresAt != null 
            && expiresAt.isAfter(LocalDateTime.now());
    }

    public boolean canBeEdited() {
        return status == JobStatus.DRAFT || status == JobStatus.PENDING_APPROVAL;
    }

    public void incrementApplicationCount() {
        this.applicationCount = (this.applicationCount == null ? 0 : this.applicationCount) + 1;
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }
}

