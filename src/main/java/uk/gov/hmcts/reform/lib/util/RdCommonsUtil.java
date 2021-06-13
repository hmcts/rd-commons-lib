package uk.gov.hmcts.reform.lib.util;

import java.lang.reflect.Field;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

import uk.gov.hmcts.reform.lib.domain.RowDomain;

public class RdCommonsUtil {

    private RdCommonsUtil() {
    }

    /**
     * get field value.
     *
     * @param field reflection field
     * @param domain domain
     * @return field value
     */
    public static String getKeyFieldValue(Field field, RowDomain domain) {
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
    public static Optional<Field> getKeyField(RowDomain bean) {
        Class<RowDomain> objectClass = (Class<RowDomain>) bean.getClass();
        Optional<Field> field = stream(objectClass.getDeclaredFields()).filter(fld ->
            nonNull(findAnnotation(fld,
                MappingField.class)) && findAnnotation(fld,
                MappingField.class).position() == 1).findFirst();
        return (field.isPresent()) ? field : Optional.empty();
    }
}
