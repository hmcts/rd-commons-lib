package uk.gov.hmcts.reform.lib.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
public class CommonExceptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exception_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "job_id")
    @NotNull
    private Long jobId;

    @Column(name = "excel_row_id")
    @Size(max = 32)
    private String excelRowId;

    @Column(name = "key")
    @Size(max = 256)
    private String keyField;

    @Column(name = "field_in_error")
    @Size(max = 32)
    private String fieldInError;

    @Column(name = "error_description")
    @Size(max = 512)
    private String errorDescription;

    @UpdateTimestamp
    @Column(name = "updated_timestamp")
    private LocalDateTime updatedTimeStamp;

}
