package uk.gov.hmcts.reform.lib.validator.service;

import java.util.List;

import uk.gov.hmcts.reform.lib.domain.RowDomain;
import uk.gov.hmcts.reform.lib.util.AuditStatus;

public interface IValidationService {
    /**
     * Returns invalid record list and JSR Constraint violations pair.
     *
     * @param rowDomainList List
     * @return iDomainList list
     */
    List<RowDomain> getInvalidRecords(List<RowDomain> rowDomainList);

    /**
     * Audit JSR exceptions..
     *
     * @param jobId long
     */
    void saveJsrExceptionsForJob(long jobId);

    /**
     * Create Audit entry with in-progress Status.
     *
     * @param auditStatus AuditStatus
     * @param fileName    String
     * @return JobId long
     */
    long startAuditing(final AuditStatus auditStatus, final String fileName);

    /**
     * Update Audit status with Success/Failure/PartialSuccess.
     *
     * @param auditStatus AuditStatus
     * @param fileName    String
     * @return JobId long
     */
    long updateAuditStatus(final AuditStatus auditStatus, final String fileName);

    long getAuditJobId();

    void logFailures(String message, long rowId);
}



