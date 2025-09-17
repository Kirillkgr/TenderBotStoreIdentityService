package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleService {

	private final RoleRepository roleRepository;


	@Transactional
	public void createRoleIfNotExists(Role.RoleName roleName) {

		if (!roleRepository.existsByName(roleName)) {
			Role role = Role.builder()
							.name(roleName)
							.build();
			roleRepository.save(role);
			log.info("Created role: {}", roleName);
		}
	}

	@Transactional(readOnly = true)
	public Role getUserRole() {

		return roleRepository.findByName(Role.RoleName.OWNER)
							 .orElseThrow(() -> new RuntimeException("Default USER role not found"));
	}


	@Transactional(readOnly = true)
	public Optional<Role> findByName(Role.RoleName name) {

		return roleRepository.findByName(name);
	}


	public Role save(Role role) {

		return roleRepository.save(role);
	}
}
