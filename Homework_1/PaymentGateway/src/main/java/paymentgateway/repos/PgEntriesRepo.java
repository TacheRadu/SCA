package paymentgateway.repos;

import org.springframework.data.repository.CrudRepository;
import paymentgateway.entities.PgEntriesEntity;

public interface PgEntriesRepo extends CrudRepository<PgEntriesEntity, Integer> {
}
