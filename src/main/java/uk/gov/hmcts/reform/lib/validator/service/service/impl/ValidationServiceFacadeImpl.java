package uk.gov.hmcts.reform.lib.validator.service.service.impl;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import uk.gov.hmcts.reform.lib.audit.domain.Audit;
import uk.gov.hmcts.reform.lib.audit.domain.ExceptionEntity;
import uk.gov.hmcts.reform.lib.util.MappingField;
import uk.gov.hmcts.reform.lib.audit.domain.RowDomain;
import uk.gov.hmcts.reform.lib.repository.AuditRepository;
import uk.gov.hmcts.reform.lib.repository.ExceptionRepository;
import uk.gov.hmcts.reform.lib.util.AuditStatus;
import uk.gov.hmcts.reform.lib.validator.service.IJsrValidatorInitializer;
import uk.gov.hmcts.reform.lib.validator.service.IValidationService;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class ValidationServiceFacadeImpl implements IValidationService {

    @Autowired
    private IJsrValidatorInitializer<RowDomain> jsrValidatorInitializer;

    private Audit audit;

    @Autowired
    AuditRepository auditRepository;

    @Autowired
    ExceptionRepository exceptionRepository;

    private long auditJobId;

    private final UserInfo userInfo;

    List<ExceptionEntity> exceptionEntities;

    @Value("${loggingComponentName}")
    private String loggingComponentName;

    public ValidationServiceFacadeImpl(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    /**
     * Returns invalid record list and JSR Constraint violations pair.
     *
     * @param rowDomains List
     * @return RowDomain list
     */
    public List<RowDomain> getInvalidRecords(List<RowDomain> rowDomains) {
        //Gets Invalid records
        return jsrValidatorInitializer.getInvalidJsrRecords(rowDomains);
    }

    /**
     * Audit JSR exceptions.
     *
     * @param jobId long
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void saveJsrExceptionsForJob(long jobId) {
        Set<ConstraintViolation<RowDomain>> constraintViolationSet
                = jsrValidatorInitializer.getConstraintViolations();
        exceptionEntities = new LinkedList<>();
        AtomicReference<Field> field = new AtomicReference<>();
        //if JSR violation present then only persist exception
        ofNullable(constraintViolationSet).ifPresent(constraintViolations ->
                constraintViolations.forEach(constraintViolation -> {
                    log.info("{}:: Invalid JSR for row Id {} in job {} ", loggingComponentName,
                            constraintViolation.getRootBean().getRowId(), jobId);
                    if (isNull(field.get())) {
                        field.set(getKeyFiled(constraintViolation.getRootBean()).get());
                        ReflectionUtils.makeAccessible(field.get());
                    }
                    ExceptionEntity exceptionEntity = new ExceptionEntity();
                    exceptionEntity.setJobId(jobId);
                    exceptionEntity.setFieldInError(constraintViolation.getPropertyPath().toString());
                    exceptionEntity.setErrorDescription(constraintViolation.getMessage());
                    exceptionEntity.setExcelRowId(String.valueOf(constraintViolation.getRootBean().getRowId()));
                    exceptionEntity.setUpdatedTimeStamp(LocalDateTime.now());
                    exceptionEntity.setKeyField(getKeyFieldValue(field.get(), constraintViolation.getRootBean()));
                    exceptionEntities.add(exceptionEntity);
                }));
        exceptionRepository.saveAll(exceptionEntities);
    }


    private String getKeyFieldValue(Field field, RowDomain domain) {
        try {
            return (String) field.get(domain);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage()); //@T0 DO replace IllegalArgumentException
        }
    }

    /**
     * get key fields.
     *
     * @param bean Object
     * @return Field Field
     */
    @SuppressWarnings("unchecked")
    private Optional<Field> getKeyFiled(RowDomain bean) {
        Class<RowDomain> objectClass = (Class<RowDomain>) bean.getClass();
        Optional<Field> field = stream(objectClass.getDeclaredFields()).filter(fld ->
                nonNull(findAnnotation(fld,
                        MappingField.class)) && findAnnotation(fld,
                        MappingField.class).position() == 1).findFirst();
        return (field.isPresent()) ? field : Optional.empty();
    }

    /**
     * Inserts Audit details in Audit table.
     *
     * @param auditStatus AuditStatus
     * @param fileName    String
     * @return long id
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public long updateAuditStatus(final AuditStatus auditStatus, final String fileName) {
        createOrUpdateAudit(auditStatus, fileName);
        this.auditJobId = auditRepository.save(audit).getJobId();
        return auditJobId;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public long startAuditing(final AuditStatus auditStatus, final String fileName) {
        this.audit = Audit.builder().build();
        createOrUpdateAudit(auditStatus, fileName);
        this.auditJobId = auditRepository.save(audit).getJobId();
        return auditJobId;
    }

    /**
     * Create ExceptionEntity domain object.
     *
     * @param jobId   long
     * @param message String
     * @return ExceptionEntity exceptionEntity
     */
    public ExceptionEntity createException(final long jobId, final String message, final Long rowId) {
        return ExceptionEntity.builder().jobId(jobId)
                .excelRowId((rowId != 0) ? rowId.toString() : "")
                .errorDescription(message).updatedTimeStamp(LocalDateTime.now()).build();
    }

    /**
     * Create/Updates Audit domain object.
     *
     * @param auditStatus AuditStatus
     * @param fileName    String
     * @return Audit audit
     */
    private Audit createOrUpdateAudit(AuditStatus auditStatus, String fileName) {
        if (isNull(audit) || isNull(audit.getJobId())) {
            String userId = (nonNull(userInfo) && nonNull(userInfo.getUid())) ? userInfo.getUid() : "";
            audit = Audit.builder()
                    .status(auditStatus.getStatus())
                    .jobStartTime(LocalDateTime.now())
                    .fileName(fileName)
                    .authenticatedUserId(userId)
                    .build();
        } else {
            audit.setStatus(auditStatus.getStatus());
            audit.setJobEndTime(LocalDateTime.now());
            audit.setJobId(getAuditJobId());
        }
        return audit;
    }


    /**
     * logging User profile failures.
     *
     * @param message String
     * @param rowId   long
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void logFailures(String message, long rowId) {
        log.info("{}:: Failure row Id {} with error {} in job {}  ", loggingComponentName, rowId, message,
                getAuditJobId());

        ExceptionEntity exceptionEntity = createException(getAuditJobId(), message, rowId);
        exceptionRepository.save(exceptionEntity);
    }

    public long getAuditJobId() {
        return auditJobId;
    }
}

