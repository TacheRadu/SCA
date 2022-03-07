package merchant.repos;

import merchant.entities.MerchantSessionsEntity;
import org.springframework.data.repository.CrudRepository;

public interface MerchantSessionsRepo extends CrudRepository<MerchantSessionsEntity, Integer> {
}
