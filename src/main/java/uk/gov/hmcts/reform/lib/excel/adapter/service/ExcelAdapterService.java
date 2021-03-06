package uk.gov.hmcts.reform.lib.excel.adapter.service;

import org.apache.poi.ss.usermodel.Workbook;
import java.util.List;

public interface ExcelAdapterService {
    <T> List<T> parseExcel(Workbook workbook, String sheetName, List<String> validHeaders, Class<T> classType);
}

