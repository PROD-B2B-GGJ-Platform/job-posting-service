package com.platform.talent.jobposting.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "job_postings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {
    
    @Id
    private String jobId;
    
    @Column(nullable = false)
    private String organizationId;
    
    private String requisitionId;
    
    @Column(nullable = false)
    private String jobTitle;
    
    @Column(length = 6000)
    private String jobDescription;
    
    @Column(length = 3000)
    private String responsibilities;
    
    @Column(length = 3000)
    private String qualifications;
    
    @Column(length = 2000)
    private String benefits;
    
    @Column(nullable = false)
    private String department;
    
    private String location;
    private String workType; // REMOTE, ONSITE, HYBRID
    private String employmentType; // FULL_TIME, PART_TIME, CONTRACT
    
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private Boolean showSalary;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPostingStatus status;
    
    private LocalDate publishedDate;
    private LocalDate expiryDate;
    
    private Integer viewCount;
    private Integer applicationCount;
    
    // Job board publishing
    private Boolean publishToLinkedIn;
    private Boolean publishToIndeed;
    private Boolean publishToGlassdoor;
    private Boolean publishToCompanyWebsite;
    
    private String linkedInJobId;
    private String indeedJobId;
    private String glassdoorJobId;
    
    private LocalDate createdDate;
    private LocalDate lastModifiedDate;
    private String createdBy;
}

