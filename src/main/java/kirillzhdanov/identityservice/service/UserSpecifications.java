package kirillzhdanov.identityservice.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import kirillzhdanov.identityservice.model.Role;
import kirillzhdanov.identityservice.model.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecifications {

    public static Specification<User> byMaster(Long masterId) {
        return (root, query, cb) -> {
            if (masterId == null) return cb.conjunction();
            return cb.equal(root.get("masterId"), masterId);
        };
    }

    public static Specification<User> byQuery(String queryText) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(queryText)) return cb.conjunction();
            String like = "%" + queryText.toLowerCase() + "%";
            Predicate byLogin = cb.like(cb.lower(root.get("username")), like);
            Predicate byFio = cb.or(
                    cb.like(cb.lower(root.get("lastName")), like),
                    cb.like(cb.lower(root.get("firstName")), like),
                    cb.like(cb.lower(root.get("patronymic")), like)
            );
            return cb.or(byLogin, byFio);
        };
    }

    public static Specification<User> byRole(String roleCode) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(roleCode)) return cb.conjunction();
            Join<User, Role> roles = root.join("roles", JoinType.LEFT);
            return cb.equal(roles.get("name"), Role.RoleName.valueOf(roleCode.toUpperCase()));
        };
    }

    public static Specification<User> byDepartment(Long departmentId) {
        return (root, query, cb) -> {
            if (departmentId == null) return cb.conjunction();
            return cb.equal(root.get("department").get("id"), departmentId);
        };
    }
}
