# Job Posting Service - Design Document Specification (DDS)

## Document Information

| Field | Value |
|-------|-------|
| Document Type | Design Document Specification |
| Version | 10.0.0.1 |
| Last Updated | 2025-12-11 |
| Status | Approved |
| Owner | Talent & Recruitment Team |

---

## 1. Overview

The Job Posting Service is a core microservice within the Talent & Recruitment Cluster responsible for managing the complete lifecycle of job postings from creation to closure.

### 1.1 Purpose

Provide a robust, scalable API for creating, managing, and publishing job postings with support for multi-tenant organizations, approval workflows, and custom fields.

### 1.2 Scope

- Job CRUD operations
- Approval workflow management
- Job publishing and expiration
- Multi-tenant data isolation
- Full-text search capabilities
- Custom fields support

---

## 2. Architecture

### 2.1 Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway                               │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│              Job Posting Service (Port 8091)                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Controller │  │   Service   │  │    Repository       │  │
│  │    Layer    │──│    Layer    │──│      Layer          │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│              PostgreSQL Database                             │
│                    (ggj_jobs table)                          │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Runtime | Java | 21+ |
| Framework | Spring Boot | 3.2.0 |
| Database | PostgreSQL | 15+ |
| ORM | Hibernate/JPA | 6.x |
| Migration | Flyway | 10.x |
| API Docs | OpenAPI/Swagger | 3.0 |
| Build Tool | Maven | 3.9+ |
| Resilience | Resilience4j | 2.x |

### 2.3 Service Dependencies

| Dependency | Purpose | Type |
|------------|---------|------|
| PostgreSQL | Data persistence | Required |
| Kafka | Event publishing | Optional |
| Redis | Caching | Optional |

---

## 3. Data Model

### 3.1 Database Schema

#### Table: ggj_jobs

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique identifier |
| organization_id | UUID | NOT NULL, INDEX | Multi-tenant isolation |
| title | VARCHAR(255) | NOT NULL | Job title |
| description | TEXT | | Job description (HTML) |
| department | VARCHAR(100) | | Department name |
| location | VARCHAR(255) | | Job location |
| employment_type | VARCHAR(50) | | FULL_TIME, PART_TIME, CONTRACT |
| salary_min | DECIMAL(15,2) | | Minimum salary |
| salary_max | DECIMAL(15,2) | | Maximum salary |
| currency | VARCHAR(3) | DEFAULT 'USD' | Currency code |
| status | VARCHAR(20) | NOT NULL | DRAFT, PENDING, APPROVED, PUBLISHED, CLOSED |
| custom_fields | JSONB | | Flexible custom fields |
| application_count | INTEGER | DEFAULT 0 | Number of applications |
| view_count | INTEGER | DEFAULT 0 | Number of views |
| published_at | TIMESTAMP | | Publication timestamp |
| expires_at | TIMESTAMP | | Expiration timestamp |
| created_by | UUID | NOT NULL | Creator user ID |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| updated_at | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

#### Indexes

```sql
CREATE INDEX idx_jobs_org_id ON ggj_jobs(organization_id);
CREATE INDEX idx_jobs_status ON ggj_jobs(status);
CREATE INDEX idx_jobs_created_at ON ggj_jobs(created_at DESC);
CREATE INDEX idx_jobs_title_gin ON ggj_jobs USING gin(to_tsvector('english', title));
```

### 3.2 Entity Relationships

```
Organization (1) ──────── (*) Job
     │
     └── User (1) ──────── (*) Job (created_by)
```

---

## 4. API Design

### 4.1 Base URL

```
/api/v1/jobs
```

### 4.2 Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/jobs` | Create new job |
| GET | `/api/v1/jobs/{id}` | Get job by ID |
| GET | `/api/v1/jobs` | List all jobs (paginated) |
| PUT | `/api/v1/jobs/{id}` | Update job |
| DELETE | `/api/v1/jobs/{id}` | Delete job |
| PUT | `/api/v1/jobs/{id}/publish` | Publish job |
| PUT | `/api/v1/jobs/{id}/close` | Close job |
| PUT | `/api/v1/jobs/{id}/approve` | Approve job |
| POST | `/api/v1/jobs/search` | Search jobs |

### 4.3 Request/Response Examples

#### Create Job Request

```json
{
  "title": "Senior Software Engineer",
  "description": "<p>We are looking for...</p>",
  "department": "Engineering",
  "location": "New York, NY",
  "employmentType": "FULL_TIME",
  "salaryMin": 120000,
  "salaryMax": 180000,
  "currency": "USD",
  "customFields": {
    "requiredExperience": "5+ years",
    "skills": ["Java", "Spring Boot", "PostgreSQL"]
  }
}
```

#### Job Response

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "organizationId": "org-123",
  "title": "Senior Software Engineer",
  "description": "<p>We are looking for...</p>",
  "department": "Engineering",
  "location": "New York, NY",
  "employmentType": "FULL_TIME",
  "salaryMin": 120000,
  "salaryMax": 180000,
  "currency": "USD",
  "status": "DRAFT",
  "applicationCount": 0,
  "viewCount": 0,
  "customFields": {
    "requiredExperience": "5+ years",
    "skills": ["Java", "Spring Boot", "PostgreSQL"]
  },
  "createdAt": "2025-12-11T10:00:00Z",
  "updatedAt": "2025-12-11T10:00:00Z"
}
```

---

## 5. Workflow Design

### 5.1 Job Status Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌───────────┐     ┌────────┐
│  DRAFT   │────▶│ PENDING  │────▶│ APPROVED │────▶│ PUBLISHED │────▶│ CLOSED │
└──────────┘     └──────────┘     └──────────┘     └───────────┘     └────────┘
     │                │                                   │
     │                │                                   │
     ▼                ▼                                   ▼
┌──────────┐     ┌──────────┐                       ┌───────────┐
│ REJECTED │     │ REJECTED │                       │  EXPIRED  │
└──────────┘     └──────────┘                       └───────────┘
```

### 5.2 Approval Workflow

1. **Draft** → Job created, can be edited
2. **Pending** → Submitted for approval
3. **Approved** → Approved by manager/HR
4. **Published** → Live on career site
5. **Closed** → Manually closed or filled
6. **Expired** → Auto-closed after expiry date

---

## 6. Security

### 6.1 Multi-Tenant Isolation

All queries include `organization_id` filter to ensure data isolation:

```java
@Query("SELECT j FROM Job j WHERE j.organizationId = :orgId")
List<Job> findByOrganizationId(@Param("orgId") UUID orgId);
```

### 6.2 Authentication

- JWT-based authentication via Keycloak
- Bearer token required for all endpoints
- Organization ID extracted from token claims

### 6.3 Authorization

| Role | Permissions |
|------|-------------|
| RECRUITER | Create, Read, Update jobs |
| HR_MANAGER | Create, Read, Update, Approve jobs |
| ADMIN | Full access including Delete |

---

## 7. Performance

### 7.1 Caching Strategy

- Job listings cached for 5 minutes
- Individual job cached for 2 minutes
- Cache invalidated on updates

### 7.2 Pagination

Default page size: 20
Maximum page size: 100

### 7.3 Database Optimization

- Indexes on frequently queried columns
- JSONB for flexible custom fields
- Connection pooling via HikariCP

---

## 8. Deployment

### 8.1 Kubernetes Resources

- Deployment with 2 replicas
- HorizontalPodAutoscaler (2-10 replicas)
- Service (ClusterIP)
- Ingress for external access

### 8.2 Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| DB_HOST | PostgreSQL host | Yes |
| DB_PORT | PostgreSQL port | Yes |
| DB_NAME | Database name | Yes |
| DB_USERNAME | Database user | Yes |
| DB_PASSWORD | Database password | Yes |
| KEYCLOAK_URL | Keycloak auth URL | Yes |

---

## 9. Monitoring

### 9.1 Health Endpoints

- `/actuator/health` - Health check
- `/actuator/metrics` - Prometheus metrics
- `/actuator/info` - Application info

### 9.2 Key Metrics

- `jobs_created_total` - Total jobs created
- `jobs_published_total` - Total jobs published
- `api_request_duration_seconds` - API latency
- `db_connection_pool_size` - Connection pool metrics

---

## 10. References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL JSONB](https://www.postgresql.org/docs/current/datatype-json.html)
- [OpenAPI Specification](https://swagger.io/specification/)

