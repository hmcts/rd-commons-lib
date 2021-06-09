package uk.gov.hmcts.reform.lib.validator.service;

import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

import uk.gov.hmcts.reform.lib.audit.domain.RowDomain;
import uk.gov.hmcts.reform.lib.util.AuditStatus;
import uk.gov.hmcts.reform.lib.util.MappingField;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

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

    public default String getKeyFieldValue(Field field, RowDomain domain) {
        try {
            return (String) field.get(domain);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    /**
     * get key fields.
     *
     * @param bean Object
     * @return Field Field
     */
    @SuppressWarnings("unchecked")
    public default Optional<Field> getKeyFiled(RowDomain bean) {
        Class<RowDomain> objectClass = (Class<RowDomain>) bean.getClass();
        Optional<Field> field = stream(objectClass.getDeclaredFields()).filter(fld ->
                nonNull(findAnnotation(fld,
                        MappingField.class)) && findAnnotation(fld,
                        MappingField.class).position() == 1).findFirst();
        return (field.isPresent()) ? field : Optional.empty();
    }
}



