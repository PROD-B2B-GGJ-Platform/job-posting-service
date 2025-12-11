package com.platform.talent.jobposting.domain.repository;

import com.platform.talent.jobposting.domain.model.Job;
import com.platform.talent.jobposting.domain.model.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByTenantIdAndStatus(UUID tenantId, JobStatus status);

    Page<Job> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Job> findByTenantIdAndStatus(UUID tenantId, JobStatus status, Pageable pageable);

    Optional<Job> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT j FROM Job j WHERE j.tenantId = :tenantId " +
           "AND j.status = 'PUBLISHED' " +
           "AND j.expiresAt > :now")
    List<Job> findActiveJobs(@Param("tenantId") UUID tenantId, 
                             @Param("now") LocalDateTime now);

    @Query("SELECT j FROM Job j WHERE j.tenantId = :tenantId " +
           "AND j.status = 'PUBLISHED' " +
           "AND j.expiresAt > :now")
    Page<Job> findActiveJobs(@Param("tenantId") UUID tenantId, 
                             @Param("now") LocalDateTime now,
                             Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.tenantId = :tenantId " +
           "AND j.recruiterId = :recruiterId")
    Page<Job> findByRecruiter(@Param("tenantId") UUID tenantId,
                               @Param("recruiterId") UUID recruiterId,
                               Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.tenantId = :tenantId " +
           "AND j.departmentId = :departmentId " +
           "AND j.status = 'PUBLISHED'")
    List<Job> findByDepartment(@Param("tenantId") UUID tenantId,
                                @Param("departmentId") UUID departmentId);

    @Query("SELECT j FROM Job j WHERE j.tenantId = :tenantId " +
           "AND (LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Job> searchJobs(@Param("tenantId") UUID tenantId,
                         @Param("keyword") String keyword,
                         Pageable pageable);

    long countByTenantIdAndStatus(UUID tenantId, JobStatus status);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.tenantId = :tenantId " +
           "AND j.expiresAt < :now " +
           "AND j.status = 'PUBLISHED'")
    long countExpiredJobs(@Param("tenantId") UUID tenantId,
                          @Param("now") LocalDateTime now);
}

