package uk.gov.hmcts.reform.lib.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class RowDomain {
    @JsonIgnore
    protected long rowId;
}

