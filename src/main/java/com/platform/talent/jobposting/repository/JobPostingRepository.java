package com.platform.talent.jobposting.repository;

import com.platform.talent.jobposting.domain.model.JobPosting;
import com.platform.talent.jobposting.domain.model.JobPostingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, String> {
    List<JobPosting> findByOrganizationId(String organizationId);
    List<JobPosting> findByOrganizationIdAndStatus(String organizationId, JobPostingStatus status);
    Optional<JobPosting> findByRequisitionId(String requisitionId);
    
    @Query("SELECT j FROM JobPosting j WHERE j.status = 'PUBLISHED' ORDER BY j.publishedDate DESC")
    List<JobPosting> findAllPublished();
    
    @Query("SELECT j FROM JobPosting j WHERE j.status = 'PUBLISHED' AND j.department = :department")
    List<JobPosting> findPublishedByDepartment(String department);
    
    @Query("SELECT j FROM JobPosting j WHERE j.status = 'PUBLISHED' AND " +
           "(LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.jobDescription) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<JobPosting> searchJobs(String keyword);
}

