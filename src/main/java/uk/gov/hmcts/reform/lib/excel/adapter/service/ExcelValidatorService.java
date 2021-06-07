package uk.gov.hmcts.reform.lib.excel.adapter.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelValidatorService {
    Workbook validateExcelFile(MultipartFile excelFile);
}
