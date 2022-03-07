package paymentgateway.repos;

import org.springframework.data.repository.CrudRepository;
import paymentgateway.entities.MerchantPksEntity;

public interface MerchantPksRepo extends CrudRepository<MerchantPksEntity, String> {
}
