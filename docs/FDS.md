# Job Posting Service - Functional Design Specification (FDS)

## Document Information

| Field | Value |
|-------|-------|
| Document Type | Functional Design Specification |
| Version | 10.0.0.1 |
| Last Updated | 2025-12-11 |
| Status | Approved |
| Owner | Talent & Recruitment Team |

---

## 1. Introduction

### 1.1 Purpose

This document describes the functional requirements and business logic for the Job Posting Service, a core component of the Talent & Recruitment Cluster.

### 1.2 Scope

The Job Posting Service enables organizations to:
- Create and manage job postings
- Implement approval workflows
- Publish jobs to career sites
- Track job performance metrics
- Support custom fields for flexibility

---

## 2. Functional Requirements

### 2.1 Job Management

#### FR-001: Create Job Posting

| Attribute | Description |
|-----------|-------------|
| Description | Users can create new job postings with required and optional fields |
| Actors | Recruiter, HR Manager, Admin |
| Preconditions | User is authenticated and has CREATE_JOB permission |
| Postconditions | Job is created in DRAFT status |

**Input Fields:**

| Field | Required | Validation |
|-------|----------|------------|
| title | Yes | 1-255 characters |
| description | No | HTML content allowed |
| department | No | 1-100 characters |
| location | No | 1-255 characters |
| employmentType | No | FULL_TIME, PART_TIME, CONTRACT, INTERN |
| salaryMin | No | Positive number |
| salaryMax | No | Must be >= salaryMin |
| currency | No | ISO 4217 code (default: USD) |
| customFields | No | Valid JSON object |

**Business Rules:**
- Job is created in DRAFT status
- createdBy is set to current user ID
- organizationId is set from user's token
- createdAt and updatedAt are set to current timestamp

---

#### FR-002: Update Job Posting

| Attribute | Description |
|-----------|-------------|
| Description | Users can update existing job postings |
| Actors | Recruiter, HR Manager, Admin |
| Preconditions | Job exists, User has UPDATE_JOB permission |
| Postconditions | Job is updated, status may change |

**Business Rules:**
- Only DRAFT and REJECTED jobs can be freely edited
- PENDING jobs can only be edited by approvers
- PUBLISHED jobs require re-approval if critical fields change
- Critical fields: title, description, salary, location

---

#### FR-003: Delete Job Posting

| Attribute | Description |
|-----------|-------------|
| Description | Admins can delete job postings |
| Actors | Admin only |
| Preconditions | Job exists, User has DELETE_JOB permission |
| Postconditions | Job is soft-deleted |

**Business Rules:**
- Jobs are soft-deleted (marked as deleted, not removed)
- Deleted jobs are not visible in listings
- Applications linked to job are preserved

---

### 2.2 Approval Workflow

#### FR-004: Submit for Approval

| Attribute | Description |
|-----------|-------------|
| Description | Submit job for approval before publishing |
| Actors | Recruiter |
| Preconditions | Job is in DRAFT status |
| Postconditions | Job status changes to PENDING |

**Business Rules:**
- Validates all required fields are filled
- Notifies approvers via email/notification
- Records submission timestamp

---

#### FR-005: Approve Job

| Attribute | Description |
|-----------|-------------|
| Description | Approve a pending job for publishing |
| Actors | HR Manager, Admin |
| Preconditions | Job is in PENDING status, User is approver |
| Postconditions | Job status changes to APPROVED |

**Business Rules:**
- Only designated approvers can approve
- Approval is logged with approver ID and timestamp
- Creator is notified of approval

---

#### FR-006: Reject Job

| Attribute | Description |
|-----------|-------------|
| Description | Reject a pending job with reason |
| Actors | HR Manager, Admin |
| Preconditions | Job is in PENDING status, User is approver |
| Postconditions | Job status changes to REJECTED |

**Input:**
- rejectionReason (required): Text explaining rejection

**Business Rules:**
- Rejection reason is mandatory
- Creator is notified with rejection reason
- Job can be edited and resubmitted

---

### 2.3 Publishing

#### FR-007: Publish Job

| Attribute | Description |
|-----------|-------------|
| Description | Publish approved job to career site |
| Actors | Recruiter, HR Manager, Admin |
| Preconditions | Job is in APPROVED status |
| Postconditions | Job status changes to PUBLISHED |

**Input:**
- expiresAt (optional): Expiration date for job

**Business Rules:**
- publishedAt is set to current timestamp
- If expiresAt is not provided, defaults to 30 days
- Job appears on public career site
- Event published to Kafka: `talent.job.published`

---

#### FR-008: Close Job

| Attribute | Description |
|-----------|-------------|
| Description | Close a published job (filled or cancelled) |
| Actors | Recruiter, HR Manager, Admin |
| Preconditions | Job is in PUBLISHED status |
| Postconditions | Job status changes to CLOSED |

**Input:**
- closeReason (optional): FILLED, CANCELLED, OTHER

**Business Rules:**
- Job is removed from career site
- Applications remain accessible
- Event published: `talent.job.closed`

---

### 2.4 Search and Filtering

#### FR-009: Search Jobs

| Attribute | Description |
|-----------|-------------|
| Description | Search jobs with filters and pagination |
| Actors | All authenticated users |
| Preconditions | User is authenticated |
| Postconditions | Paginated list of matching jobs |

**Search Criteria:**

| Filter | Type | Description |
|--------|------|-------------|
| keyword | String | Full-text search in title and description |
| status | String[] | Filter by status |
| department | String | Filter by department |
| location | String | Filter by location |
| employmentType | String | Filter by employment type |
| salaryMin | Number | Minimum salary filter |
| salaryMax | Number | Maximum salary filter |
| createdAfter | DateTime | Jobs created after date |
| createdBefore | DateTime | Jobs created before date |

**Pagination:**
- page: Page number (default: 0)
- size: Page size (default: 20, max: 100)
- sort: Sort field and direction

---

### 2.5 Custom Fields

#### FR-010: Custom Fields Support

| Attribute | Description |
|-----------|-------------|
| Description | Support flexible custom fields per organization |
| Actors | All users |
| Preconditions | None |
| Postconditions | Custom fields stored as JSONB |

**Examples:**
```json
{
  "requiredExperience": "5+ years",
  "skills": ["Java", "Spring Boot"],
  "workAuthorization": "US Citizen or Green Card",
  "travelRequirement": "25%",
  "remoteOption": true
}
```

**Business Rules:**
- Custom fields are optional
- Stored as JSONB for flexibility
- No schema validation (client responsibility)
- Searchable via PostgreSQL JSONB operators

---

## 3. Non-Functional Requirements

### 3.1 Performance

| Requirement | Target |
|-------------|--------|
| API Response Time | < 200ms (p95) |
| Concurrent Users | 1000+ |
| Jobs per Organization | 10,000+ |
| Search Response | < 500ms |

### 3.2 Availability

| Requirement | Target |
|-------------|--------|
| Uptime | 99.9% |
| RTO | 1 hour |
| RPO | 15 minutes |

### 3.3 Security

- All data encrypted in transit (TLS 1.3)
- Sensitive data encrypted at rest
- Row-level security via organization_id
- Audit logging for all operations

---

## 4. Integration Points

### 4.1 Inbound

| Source | Integration | Purpose |
|--------|-------------|---------|
| Requisition Service | REST API | Create job from approved requisition |
| Career Site | REST API | Fetch published jobs |

### 4.2 Outbound

| Target | Integration | Purpose |
|--------|-------------|---------|
| Kafka | Event | Publish job lifecycle events |
| Application Tracking | Event | Notify of job status changes |
| Job Boards | REST API | Syndicate to external boards |

### 4.3 Events Published

| Event | Trigger | Payload |
|-------|---------|---------|
| talent.job.created | Job created | Job ID, title, org ID |
| talent.job.published | Job published | Job ID, title, location |
| talent.job.closed | Job closed | Job ID, reason |
| talent.job.updated | Job updated | Job ID, changed fields |

---

## 5. User Interface Requirements

### 5.1 Recruiter Portal Integration

The service provides APIs consumed by:
- Job List View: Paginated job listings
- Job Detail View: Full job information
- Job Form: Create/Edit job posting
- Approval Queue: Pending jobs for approval

### 5.2 Career Site Integration

Public APIs for:
- Published job listings
- Job detail page
- Job search and filtering

---

## 6. Data Retention

| Data Type | Retention Period |
|-----------|------------------|
| Active Jobs | Indefinite |
| Closed Jobs | 7 years |
| Deleted Jobs | 90 days |
| Audit Logs | 7 years |

---

## 7. Acceptance Criteria

### 7.1 Job Creation

- [ ] Job can be created with required fields only
- [ ] Job is created in DRAFT status
- [ ] Custom fields are stored correctly
- [ ] Validation errors return 400 with details

### 7.2 Approval Workflow

- [ ] Only DRAFT jobs can be submitted for approval
- [ ] Only approvers can approve/reject
- [ ] Rejection requires reason
- [ ] Notifications sent on status change

### 7.3 Publishing

- [ ] Only APPROVED jobs can be published
- [ ] Published jobs visible on career site
- [ ] Jobs auto-expire after expiration date
- [ ] Events published to Kafka

### 7.4 Search

- [ ] Full-text search works on title/description
- [ ] All filters work correctly
- [ ] Pagination works correctly
- [ ] Sort by multiple fields supported

---

## 8. Glossary

| Term | Definition |
|------|------------|
| Job Posting | A job advertisement created by an organization |
| Requisition | A formal request to hire for a position |
| Approval Workflow | Process to review and approve job before publishing |
| Custom Fields | Organization-specific fields stored as JSONB |
| Career Site | Public-facing website showing job listings |

