package paymentgateway.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import paymentgateway.entities.PgEntriesEntity;

import java.util.Optional;

public interface PgEntriesRepo extends CrudRepository<PgEntriesEntity, Integer> {
    @Query(value =
            "SELECT * FROM pg_entries " +
                    "WHERE sid = ?1 " +
                    "AND amount = ?2 " +
                    "AND nounce = ?3 " +
                    "AND pkc = ?4",
            nativeQuery = true)
    Optional<PgEntriesEntity> findByPk(int sid, double amount, int nonce, String pk);
}
