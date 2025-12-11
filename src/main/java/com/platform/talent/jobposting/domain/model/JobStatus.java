package com.platform.talent.jobposting.domain.model;

public enum JobStatus {
    DRAFT,          // Job is being created/edited
    PENDING_APPROVAL, // Awaiting approval
    APPROVED,       // Approved but not yet published
    PUBLISHED,      // Live and accepting applications
    CLOSED,         // No longer accepting applications
    CANCELLED,      // Cancelled before publishing
    ARCHIVED        // Archived for record keeping
}

