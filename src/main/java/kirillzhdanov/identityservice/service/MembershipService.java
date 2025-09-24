package kirillzhdanov.identityservice.service;

public interface MembershipService {
    void ensureMembershipForUsernameInCurrentBrand(String username);
}
