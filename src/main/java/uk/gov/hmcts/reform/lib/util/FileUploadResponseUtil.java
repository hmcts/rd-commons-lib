package uk.gov.hmcts.reform.lib.util;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.AND;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.RECORDS_FAILED;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.RECORDS_UPLOADED;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.REQUEST_COMPLETED_SUCCESSFULLY;
import static uk.gov.hmcts.reform.lib.util.ExcelAdapterConstants.REQUEST_FAILED_FILE_UPLOAD_JSR;

import uk.gov.hmcts.reform.lib.domain.CommonExceptionEntity;
import uk.gov.hmcts.reform.lib.domain.CommonFileUploadResponse;
import uk.gov.hmcts.reform.lib.domain.JsrFileErrors;

public class FileUploadResponseUtil {

    private FileUploadResponseUtil() {
    }

    /**
     * create LrdFileUploadResponse.
     *
     * @return LrdFileUploadResponse lrdFileUploadResponse
     */
    public static CommonFileUploadResponse createResponse(int totalRecords, List<? extends CommonExceptionEntity>
        exceptionLrdList) {
        var fileUploadResponseBuilder =
            CommonFileUploadResponse.builder();

        if (isNotEmpty(exceptionLrdList)) {

            Map<String, List<CommonExceptionEntity>> failedRecords = exceptionLrdList.stream()
                .collect(groupingBy(CommonExceptionEntity::getExcelRowId));

            LinkedList<JsrFileErrors> jsrFileErrors = new LinkedList<>();

            failedRecords.entrySet().stream()
                .sorted(Comparator.comparingInt(s -> Integer.valueOf(s.getKey())))
                .forEachOrdered(map ->
                    map.getValue().forEach(jsrInvalid ->
                        jsrFileErrors.add(JsrFileErrors.builder()
                            .rowId(jsrInvalid.getExcelRowId())
                            .errorDescription(
                                jsrInvalid
                                    .getErrorDescription())
                            .filedInError(
                                jsrInvalid.getFieldInError())
                            .build())));

            String detailedMessage = constructDetailedMessage(totalRecords, failedRecords.size());
            return fileUploadResponseBuilder.message(REQUEST_FAILED_FILE_UPLOAD_JSR)
                .detailedMessage(detailedMessage).errorDetails(jsrFileErrors).build();
        } else {
            StringJoiner detailedMessage = new StringJoiner(" " + AND + " ");
            //get the uploaded records excluding suspended records
            int noOfUploadedRecords = totalRecords;

            if (noOfUploadedRecords > 0) {
                detailedMessage.add(format(RECORDS_UPLOADED, noOfUploadedRecords));
            }

            return fileUploadResponseBuilder
                .message(REQUEST_COMPLETED_SUCCESSFULLY)
                .detailedMessage(detailedMessage.toString()).build();
        }
    }

    public static String constructDetailedMessage(int totalRecords, int failedRecordsCount) {
        String detailedMessage = format(RECORDS_FAILED, failedRecordsCount);
        //get the uploaded records excluding failed records
        int uploadedRecords = totalRecords - failedRecordsCount;
        if (uploadedRecords > 0) {
            detailedMessage = format(RECORDS_FAILED, failedRecordsCount) + " " + AND + " "
                .concat(format(RECORDS_UPLOADED, uploadedRecords));
        }
        return detailedMessage;
    }
}
