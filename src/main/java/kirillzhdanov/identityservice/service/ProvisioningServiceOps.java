package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;

public interface ProvisioningServiceOps {
    MasterAccount ensureMasterAccountForUser(User user);
    void ensureOwnerMembership(User user, MasterAccount master);
    void ensureDefaultBrandAndPickup(User user, MasterAccount master);
}
