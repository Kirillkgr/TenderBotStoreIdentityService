package kirillzhdanov.identityservice.config;

import jakarta.annotation.PostConstruct;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {

	private final RoleRepository roleRepository;

	@PostConstruct
	@Transactional
	public void init(){

		createRoleIfNotExists(Role.RoleName.USER);
		createRoleIfNotExists(Role.RoleName.ADMIN);
		createRoleIfNotExists(Role.RoleName.OWNER);
		log.info("Database initialized with default roles");
	}

	private void createRoleIfNotExists(Role.RoleName roleName){

		if(!roleRepository.existsByName(roleName)) {
			Role role = Role.builder()
								.name(roleName)
								.build();
			roleRepository.save(role);
			log.info("Created role: {}", roleName);
		}
	}
}
