package uk.gov.hmcts.reform.lib.domain;

import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode

public class CommonFileUploadResponse {
    @JsonProperty("message")
    private String message;

    @JsonProperty("message_details")
    private String detailedMessage;

    @JsonProperty("error_details")
    private LinkedList<JsrFileErrors> errorDetails;
}
