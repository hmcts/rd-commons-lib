package uk.gov.hmcts.reform.lib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.lib.audit.domain.Audit;

@Repository
public interface AuditRepository extends JpaRepository<Audit, String> {

    @Query(value = "select count(*) from audit where job_start_time\\:\\:date "
            + " >= current_date - 1  and authenticated_user_id = :authenticatedUserId  and status = :status "
            + "and file_name = :fileName",  nativeQuery = true)
    long findByAuthenticatedUserIdAndStatus(String authenticatedUserId, String status, String fileName);
}
