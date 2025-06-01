package kirillzhdanov.identityservice.service;

import jakarta.annotation.PostConstruct;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {

	private final RoleRepository roleRepository;

	@PostConstruct
	public void init(){
		// Initialize default roles if they don't exist
		createRoleIfNotExists(Role.RoleName.USER);
		createRoleIfNotExists(Role.RoleName.ADMIN);
		createRoleIfNotExists(Role.RoleName.OWNER);
	}

	@Transactional
	public void createRoleIfNotExists(Role.RoleName roleName){

		if(!roleRepository.existsByName(roleName)) {
			Role role = Role.builder()
								.name(roleName)
								.build();
			roleRepository.save(role);
		}
	}

	@Transactional(readOnly = true)
	public Role getUserRole(){

		return roleRepository.findByName(Role.RoleName.USER)
					   .orElseThrow(()->new RuntimeException("Default USER role not found"));
	}

	@Transactional(readOnly = true)
	public List<Role> getAllRoles(){

		return roleRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<Role> findByName(Role.RoleName name){

		return roleRepository.findByName(name);
	}
}
