-- Job Posting Service - Initial Schema
-- Version: 10.0.0.1

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE ggj_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(100),
    employment_type VARCHAR(50),
    experience_level VARCHAR(50),
    salary_min NUMERIC(15, 2),
    salary_max NUMERIC(15, 2),
    salary_currency VARCHAR(3),
    status VARCHAR(50) NOT NULL,
    department_id UUID,
    recruiter_id UUID NOT NULL,
    hiring_manager_id UUID,
    number_of_positions INTEGER,
    published_at TIMESTAMP,
    expires_at TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by UUID,
    custom_fields JSONB,
    requirements JSONB,
    benefits JSONB,
    application_count INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    is_remote BOOLEAN DEFAULT FALSE,
    is_featured BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_salary CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min)
);

-- Indexes for performance
CREATE INDEX idx_job_tenant_status ON ggj_jobs(tenant_id, status);
CREATE INDEX idx_job_published_at ON ggj_jobs(published_at);
CREATE INDEX idx_job_expires_at ON ggj_jobs(expires_at);
CREATE INDEX idx_job_recruiter ON ggj_jobs(recruiter_id);
CREATE INDEX idx_job_department ON ggj_jobs(department_id);
CREATE INDEX idx_job_status ON ggj_jobs(status);

-- Full-text search index
CREATE INDEX idx_job_title_search ON ggj_jobs USING GIN(to_tsvector('english', title));
CREATE INDEX idx_job_description_search ON ggj_jobs USING GIN(to_tsvector('english', description));

-- JSONB indexes for custom fields
CREATE INDEX idx_job_custom_fields ON ggj_jobs USING GIN(custom_fields);
CREATE INDEX idx_job_requirements ON ggj_jobs USING GIN(requirements);

-- Comments
COMMENT ON TABLE ggj_jobs IS 'Job postings with approval workflow';
COMMENT ON COLUMN ggj_jobs.tenant_id IS 'Multi-tenant identifier';
COMMENT ON COLUMN ggj_jobs.status IS 'DRAFT, PENDING_APPROVAL, APPROVED, PUBLISHED, CLOSED, CANCELLED, ARCHIVED';
COMMENT ON COLUMN ggj_jobs.custom_fields IS 'Extended attributes stored as JSONB';

