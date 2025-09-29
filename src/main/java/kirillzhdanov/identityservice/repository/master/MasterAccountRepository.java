package kirillzhdanov.identityservice.repository.master;

import kirillzhdanov.identityservice.model.master.MasterAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterAccountRepository extends JpaRepository<MasterAccount, Long> {
    Optional<MasterAccount> findByName(String name);
}
