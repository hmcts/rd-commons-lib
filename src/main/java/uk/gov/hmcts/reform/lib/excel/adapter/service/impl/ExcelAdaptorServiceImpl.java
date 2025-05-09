package uk.gov.hmcts.reform.lib.excel.adapter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.lib.excel.adapter.service.ExcelAdapterService;
import uk.gov.hmcts.reform.lib.exception.ExcelValidationException;
import uk.gov.hmcts.reform.lib.util.MappingField;
import uk.gov.hmcts.reform.lib.validator.service.IValidationService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.poi.ss.usermodel.DateUtil.getLocalDateTime;
import static org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.DELIMITER_COMMA;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.ERROR_FILE_PARSING_ERROR_MESSAGE;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.ERROR_PARSING_EXCEL_CELL_ERROR_MESSAGE;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.FILE_MISSING_HEADERS;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.FILE_NO_DATA_ERROR_MESSAGE;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.FILE_NO_VALID_SHEET_ERROR_MESSAGE;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.IS_PRIMARY_FIELD;

@Service
@Slf4j
@SuppressWarnings("unchecked")
public class ExcelAdaptorServiceImpl implements ExcelAdapterService {

    @Value("${loggingComponentName}")
    private String loggingComponentName;

    private FormulaEvaluator evaluator;

    @Autowired
    IValidationService validationServiceFacade;


    public <T> List<T> parseExcel(Workbook workbook, String sheetName, List<String> validHeaders, Class<T> classType) {
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();

        Sheet sheet = workbook.getSheet(sheetName);
        if (Objects.isNull(sheet)) {
            throw new ExcelValidationException(HttpStatus.BAD_REQUEST, FILE_NO_VALID_SHEET_ERROR_MESSAGE);
        } else if (sheet.getPhysicalNumberOfRows() < 2) { // check at least 1 row
            throw new ExcelValidationException(HttpStatus.BAD_REQUEST, FILE_NO_DATA_ERROR_MESSAGE);
        }
        List<String> headers = new LinkedList<>();
        collectHeaderList(headers, sheet);
        validateHeaders(headers, validHeaders);
        return mapToPojo(sheet, classType, headers);
    }

    private void validateHeaders(List<String> headersToBeValidated,
                                 List<String> validHeaders) {
        validHeaders.forEach(acceptableHeader -> {
            if (!headersToBeValidated.contains(acceptableHeader)) {
                log.error("{}::{}:: Job Id {}", loggingComponentName, FILE_MISSING_HEADERS,
                        validationServiceFacade.getAuditJobId());
                throw new ExcelValidationException(HttpStatus.BAD_REQUEST, FILE_MISSING_HEADERS);
            }
        });
    }

    private <T> List<T> mapToPojo(Sheet sheet, Class<T> classType, List<String> headers) {
        List<T> objectList = new ArrayList<>();
        Map<String, Object> childHeaderToCellMap = new HashMap<>();
        Map<String, Field> parentFieldMap = new HashMap<>();
        //scan parent and domain object fields by reflection and make maps
        List<Triple<String, Field, List<Field>>> customObjectFieldsMapping =
                createBeanFieldMaps(classType, parentFieldMap);
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next();//skip header
        Field rowField = getRowIdField((Class<Object>) classType);
        Optional<Object> bean;
        int blankRowCount = 0;
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            try {
                if (isNotTrue(checkIfRowIsEmpty(row))) {
                    bean = handleRowProcessing(classType, rowField, headers, row, parentFieldMap,
                            childHeaderToCellMap, customObjectFieldsMapping);
                    bean.ifPresent(o -> objectList.add((T) o));
                } else {
                    blankRowCount++;
                }
            } catch (Exception ex) {
                validationServiceFacade.logFailures(format(ERROR_PARSING_EXCEL_CELL_ERROR_MESSAGE, ex.getMessage()),
                        row.getRowNum());
            }
        }

        //throw exception if all rows in the file are blank
        if (blankRowCount == sheet.getLastRowNum()) {
            throw new ExcelValidationException(HttpStatus.BAD_REQUEST, FILE_NO_DATA_ERROR_MESSAGE);
        }
        return objectList;
    }


    public <T> Optional<Object> handleRowProcessing(Class<T> classType, Field rowField, List<String> headers, Row row,
                                                    Map<String, Field> parentFieldMap,
                                                    Map<String, Object> childHeaderToCellMap,
                                                    List<Triple<String, Field, List<Field>>>
                                                            customObjectFieldsMapping) {
        Object bean;
        try {
            bean = populateDomainObject(classType, rowField, headers, row, parentFieldMap, childHeaderToCellMap,
                    customObjectFieldsMapping);
        } catch (Exception ex) {
            validationServiceFacade.logFailures(format(ERROR_PARSING_EXCEL_CELL_ERROR_MESSAGE, ex.getMessage()),
                    row.getRowNum());
            return empty();
        }
        return ofNullable(bean);
    }

    public <T> Object populateDomainObject(Class<T> classType, Field rowField, List<String> headers, Row row,
                                           Map<String, Field> parentFieldMap, Map<String, Object> childHeaderToCellMap,
                                           List<Triple<String, Field, List<Field>>> customObjectFieldsMapping) {
        Object bean = getInstanceOf(classType.getName());//create parent object
        //Incrementing the row id by 1 because in the excel, first row will always contain headers.
        //Actual record containing user details will be starting from row 2.
        setFieldValue(rowField, bean, row.getRowNum() + 1);// set row id to parent field
        for (int i = 0; i < headers.size(); i++) { //set all parent fields
            setParentFields(getCellValue(row.getCell(i)), bean, headers.get(i), parentFieldMap, childHeaderToCellMap);
        }
        populateChildDomainObjects(bean, customObjectFieldsMapping, childHeaderToCellMap);
        return bean;
    }

    private void populateChildDomainObjects(
            Object parentBean, List<Triple<String, Field, List<Field>>> customObjectFields,
            Map<String, Object> childHeaderValues) {
        customObjectFields.forEach(customObjectTriple -> {
            Field parentField = customObjectTriple.getMiddle();
            List<Object> domainObjectList = new ArrayList<>();
            int objectCount = findAnnotation(parentField, MappingField.class).objectCount();//take count from parent
            for (int i = 0; i < objectCount; i++) {

                //getInstanceOf(customObjectTriple.getLeft());//instantiate child domain object
                Object childDomainObject = null;
                for (Field childField : customObjectTriple.getRight()) {
                    MappingField mappingField = findAnnotation(childField, MappingField.class);
                    childDomainObject = getChildObject(childHeaderValues, customObjectTriple, i,
                            childDomainObject, childField, mappingField);
                }
                if (nonNull(childDomainObject)) {
                    domainObjectList.add(childDomainObject); //add populated child domain object into list
                }
            }
            setFieldValue(parentField, parentBean, domainObjectList);//finally set list to parent field
        });
    }

    private Object getChildObject(Map<String, Object> childHeaderValues, Triple<String,
            Field, List<Field>> customObjectTriple, int i, Object childDomainObject, Field childField,
                                  MappingField mappingField) {
        if (nonNull(mappingField)) {
            String domainObjectColumnName = mappingField.columnName().split(DELIMITER_COMMA)[i].trim();
            Object fieldValue = childHeaderValues.get(domainObjectColumnName);
            if (nonNull(fieldValue) && isNotEmpty(fieldValue.toString())) {
                childDomainObject = isNull(childDomainObject) ? getInstanceOf(customObjectTriple.getLeft())
                        : childDomainObject;
                setFieldValue(childField, childDomainObject, fieldValue);
                setIsPrimaryField(childDomainObject, mappingField, domainObjectColumnName);
            }

        }
        return childDomainObject;
    }

    //called once per file only
    private <T> List<Triple<String, Field, List<Field>>> createBeanFieldMaps(Class<T> objectClass,
                                                                             Map<String, Field> headerToCellValueMap) {
        List<Triple<String, Field, List<Field>>> customObjects = new ArrayList<>();
        for (Field field : objectClass.getDeclaredFields()) {
            MappingField mappingField = findAnnotation(field, MappingField.class);
            if (isNull(mappingField)) {
                // do nothing
            } else if (!(mappingField.columnName().isEmpty())) {
                headerToCellValueMap.put(mappingField.columnName(), field);
            } else {
                // make triple of child domain object class name, parent field, respective list of domain object fields
                customObjects.add(Triple.of(mappingField.clazz().getCanonicalName(), field,
                        asList(mappingField.clazz().getDeclaredFields())));
            }
        }
        return customObjects;
    }

    private void collectHeaderList(List<String> headers, Sheet sheet) {
        Row row = sheet.getRow(0);
        Iterator<Cell> headerIterator = row.cellIterator();
        while (headerIterator.hasNext()) {
            Cell cell = headerIterator.next();
            headers.add(cell.getStringCellValue().trim());
        }
    }

    private void setFieldValue(Field field, Object bean, Object value) {
        if (nonNull(field) && nonNull(value) && isNotEmpty(value.toString())) {
            makeAccessible(field);
            setField(field, bean, value);
        }
    }

    private Object getInstanceOf(String className) {
        Object objectInstance = null;
        try {
            objectInstance = Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throwFileParsingException();
        }
        return objectInstance;
    }

    private void setParentFields(Object cellValue, Object bean, String header, Map<String, Field> fieldHashMap,
                                 Map<String, Object> childHeaderValues) {
        Field field = fieldHashMap.get(header);
        if (nonNull(field)) {
            setFieldValue(field, bean, cellValue);
        } else {
            childHeaderValues.put(header, cellValue);
        }
    }

    private void setIsPrimaryField(Object childDomainObject, MappingField mappingField, String domainObjectColumnName) {
        if (nonNull(mappingField.isPrimary()) && domainObjectColumnName.equals(mappingField.isPrimary())) {
            try {
                Field primaryField = childDomainObject.getClass().getDeclaredField(IS_PRIMARY_FIELD);
                setFieldValue(primaryField, childDomainObject, true);
            } catch (NoSuchFieldException e) {
                throwFileParsingException();
            }
        }
    }

    private Object getCellValue(Cell cell) {
        if (isNull(cell)) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC: {
                if (isCellDateFormatted(cell)) {
                    return getLocalDateTime(cell.getNumericCellValue());
                } else {
                    return Integer.valueOf((int) cell.getNumericCellValue());
                }
            }
            case FORMULA:
                return getValueFromFormula(cell);
            default:
                return null;
        }
    }
    //This method has been added for functional test purpose.
    //It should be removed before deploying to production

    private Object getValueFromFormula(Cell cell) {
        switch (evaluator.evaluateFormulaCell(cell)) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                return cell.getStringCellValue();
            default:
                return null;
        }
    }

    private Field getRowIdField(Class<Object> classType) {
        try {
            return classType.getSuperclass().getDeclaredField("rowId");
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("invalid Row exception");
        }
    }

    private void throwFileParsingException() {
        throw new ExcelValidationException(INTERNAL_SERVER_ERROR, ERROR_FILE_PARSING_ERROR_MESSAGE);
    }

    private boolean checkIfRowIsEmpty(Row row) {
        if (row == null) {
            return true;
        }
        if (row.getLastCellNum() <= 0) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            Object cellValue = getCellValue(cell);
            if (nonNull(cellValue) && isNotEmpty(cellValue.toString().trim())) {
                return false;
            }
        }
        return true;
    }
}
