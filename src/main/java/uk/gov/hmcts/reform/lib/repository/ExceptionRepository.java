package uk.gov.hmcts.reform.lib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.lib.audit.domain.ExceptionEntity;
import java.util.List;

@Repository
public interface ExceptionRepository extends JpaRepository<ExceptionEntity, Long> {

    List<ExceptionEntity> findByJobId(Long jobId);
}
