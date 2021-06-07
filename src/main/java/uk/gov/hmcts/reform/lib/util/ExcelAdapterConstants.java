package uk.gov.hmcts.reform.lib.util;

public final class ExcelAdapterConstants {

    private ExcelAdapterConstants() {
        super();
    }

    //excel adapter related error messages
    public static final String FILE_NOT_EXCEL_TYPE_ERROR_MESSAGE = "You can only upload xlsx or xls files."
            + " Check it’s saved in the correct format and try again.";
    public static final String FILE_NOT_PRESENT_ERROR_MESSAGE = "You can only upload xlsx or xls files."
            + " Check it’s saved in the correct format and try again.";
    public static final String FILE_PASSWORD_PROTECTED_ERROR_MESSAGE =
            "The file is password protected. Please provide a file without password.";
    public static final String FILE_NO_VALID_SHEET_ERROR_MESSAGE = "The uploaded file does not "
            + "contain the ‘Staff data’ worksheet.";
    public static final String ERROR_FILE_PARSING_ERROR_MESSAGE = "Error while parsing ";
    public static final String INVALID_EXCEL_FILE_ERROR_MESSAGE = "Excel File is invalid";
    public static final String ERROR_PARSING_EXCEL_FILE_ERROR_MESSAGE = "Excel File is invalid";

    public static final String ERROR_PARSING_EXCEL_CELL_ERROR_MESSAGE = "Error parsing excel cell : %s";

    public static final String FILE_NO_DATA_ERROR_MESSAGE = "There is no data in the file uploaded."
            + " Upload a valid file in xlsx or xls format";
    public static final String FILE_MISSING_HEADERS = "The file is missing some column headers."
            + " Review and upload again";
    public static final String REQUEST_FAILED_FILE_UPLOAD_JSR =
            "Request completed with partial success. Some records failed during validation and were ignored.";

    public static final String TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String TYPE_XLS = "application/vnd.ms-excel";
    public static final String CLASS_XSSF_WORKBOOK_FACTORY = "org.apache.poi.xssf.usermodel.XSSFWorkbookFactory";
    public static final String CLASS_HSSF_WORKBOOK_FACTORY = "org.apache.poi.hssf.usermodel.HSSFWorkbookFactory";
    public static final String IS_PRIMARY_FIELD = "isPrimary";
    public static final String EMPTY = "";
    public static final String DELIMITER_COMMA = ",";
    public static final String RECORDS_UPLOADED = "%s record(s) uploaded";
    public static final String RECORDS_FAILED = "%s record(s) failed";
    public static final String SPACE = " ";
    public static final String AND = "and";


}
