package kirillzhdanov.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import kirillzhdanov.identityservice.dto.BrandDto;
import kirillzhdanov.identityservice.security.RbacGuard;
import kirillzhdanov.identityservice.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для управления брендами.
 *
 * <p>Предоставляет CRUD операции для работы с брендами, а также операции по назначению пользователей брендам.</p>
 */
@RestController
@RequestMapping("/auth/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;
    private final RbacGuard rbacGuard;

    /**
     * Получает список всех брендов.
     *
     * @return {@code ResponseEntity} со списком брендов и HTTP статусом {@code 200 OK}
     */
    @GetMapping
    @Operation(summary = "Мои бренды", description = "Требования: аутентификация. Возвращает бренды в пределах текущего master контекста.")
    public ResponseEntity<List<BrandDto>> getAllBrands() {

        return ResponseEntity.ok(brandService.getMyBrands());
    }

    /**
     * Получает бренд по его идентификатору.
     *
     * @param id идентификатор бренда
     * @return {@code ResponseEntity} с данными бренда и HTTP статусом:
     * {@code 200 OK} - бренд найден;
     * {@code 404 Not Found} - бренд с указанным ID не найден.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Бренд по ID", description = "Требования: аутентификация. Доступ только к своим брендам (чужие → 404).")
    public ResponseEntity<BrandDto> getBrandById(@PathVariable Long id) {

        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    /**
     * Создает новый бренд.
     *
     * @param brandDto данные нового бренда
     * @return {@code ResponseEntity} с созданным брендом и HTTP статусом {@code 201 Created}
     */
    @PostMapping
    @Operation(summary = "Создать бренд", description = "Требования: AUTH (любой аутентифицированный пользователь). Роль: CLIENT+ (будет владельцем созданного бренда).")
    public ResponseEntity<BrandDto> createBrand(@RequestBody BrandDto brandDto) {
        // Любой аутентифицированный пользователь может создать свой бренд (вариант Б)
        rbacGuard.requireAuthenticated();
        return new ResponseEntity<>(brandService.createBrand(brandDto), HttpStatus.CREATED);
    }

    /**
     * Обновляет данные бренда.
     *
     * @param id       идентификатор обновляемого бренда
     * @param brandDto новые данные бренда
     * @return {@code ResponseEntity} с обновленным брендом и HTTP статусом {@code 200 OK};
     * {@code 404 Not Found} - бренд с указанным ID не найден.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Обновить бренд", description = "Требования: роль OWNER или ADMIN активного membership. Чужой бренд → 404; недостаточно прав → 403.")
    public ResponseEntity<BrandDto> updateBrand(@PathVariable Long id, @RequestBody BrandDto brandDto) {
        rbacGuard.requireOwnerOrAdmin();
        return ResponseEntity.ok(brandService.updateBrand(id, brandDto));
    }

    /**
     * Удаляет бренд по его идентификатору.
     *
     * @param id идентификатор удаляемого бренда
     * @return {@code ResponseEntity} с HTTP статусом {@code 204 No Content};
     * {@code 404 Not Found} - бренд с указанным ID не найден.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить бренд", description = "Требования: роль OWNER или ADMIN активного membership. Чужой бренд → 404; недостаточно прав → 403.")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        rbacGuard.requireOwnerOrAdmin();
        brandService.deleteBrand(id);
        return ResponseEntity.noContent()
                .build();
    }

    /**
     * Назначает пользователя бренду.
     *
     * @param userId  идентификатор пользователя
     * @param brandId идентификатор бренда
     * @return {@code ResponseEntity} с HTTP статусом {@code 200 OK};
     * {@code 404 Not Found} - пользователь или бренд не найдены.
     */
    @PostMapping("/{brandId}/users/{userId}")
    @Operation(summary = "Назначить пользователя бренду", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<Void> assignUserToBrand(@PathVariable Long userId, @PathVariable Long brandId) {
        rbacGuard.requireOwnerOrAdmin();
        brandService.assignUserToBrand(userId, brandId);
        return ResponseEntity.ok()
                .build();
    }

    /**
     * Удаляет пользователя из бренда.
     *
     * @param userId  идентификатор пользователя
     * @param brandId идентификатор бренда
     * @return {@code ResponseEntity} с HTTP статусом {@code 200 OK};
     * {@code 404 Not Found} - пользователь или бренд не найдены;
     * {@code 400 Bad Request} - пользователь не назначен на бренд.
     */
    @DeleteMapping("/{brandId}/users/{userId}")
    @Operation(summary = "Удалить пользователя из бренда", description = "Требования: роль OWNER или ADMIN активного membership.")
    public ResponseEntity<Void> removeUserFromBrand(@PathVariable Long userId, @PathVariable Long brandId) {
        rbacGuard.requireOwnerOrAdmin();
        brandService.removeUserFromBrand(userId, brandId);
        return ResponseEntity.ok()
                .build();
    }
}
