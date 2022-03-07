package merchant.repos;

import merchant.entities.MerchantSessionsEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

public interface MerchantSessionsRepo extends CrudRepository<MerchantSessionsEntity, Long> {
}
