package kirillzhdanov.identityservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import kirillzhdanov.identityservice.dto.inventory.SupplierDto;
import kirillzhdanov.identityservice.exception.BadRequestException;
import kirillzhdanov.identityservice.exception.ResourceNotFoundException;
import kirillzhdanov.identityservice.model.inventory.Supplier;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.repository.inventory.SupplierRepository;
import kirillzhdanov.identityservice.tenant.ContextAccess;
import kirillzhdanov.identityservice.util.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public SupplierDto create(SupplierDto dto) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        validate(dto);
        String name = dto.getName().trim();
        if (supplierRepository.existsByMaster_IdAndNameIgnoreCase(masterId, name)) {
            throw new BadRequestException("Supplier with this name already exists");
        }
        Supplier s = new Supplier();
        s.setMaster(em.getReference(MasterAccount.class, masterId));
        return setSupplierDataToDto(dto, s, name);
    }

    @Transactional
    public SupplierDto update(Long id, SupplierDto dto) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        validate(dto);
        Supplier s = supplierRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        String name = dto.getName().trim();
        if (!name.equalsIgnoreCase(s.getName()) && supplierRepository.existsByMaster_IdAndNameIgnoreCase(masterId, name)) {
            throw new BadRequestException("Supplier with this name already exists");
        }
        return setSupplierDataToDto(dto, s, name);
    }


    @Transactional
    public void delete(Long id) {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        Supplier s = supplierRepository.findByIdAndMaster_Id(id, masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        supplierRepository.delete(s);
    }

    @Transactional
    public List<SupplierDto> list() {
        Long masterId = ContextAccess.getMasterIdOrThrow();
        return supplierRepository.findAllByMaster_Id(masterId)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    private void validate(SupplierDto dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("Supplier name is required");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String email = dto.getEmail().trim();
            if (email.length() > 255 || !email.contains("@")) {
                throw new BadRequestException("Invalid email");
            }
        }
        if (dto.getPhone() != null && dto.getPhone().trim().length() > 64) {
            throw new BadRequestException("Phone too long");
        }
    }

    @NotNull
    private SupplierDto setSupplierDataToDto(SupplierDto dto, Supplier s, String name) {
        s.setName(name);
        s.setPhone(TextUtils.trimToNull(dto.getPhone()));
        s.setEmail(TextUtils.trimToNull(dto.getEmail()));
        s.setAddress(TextUtils.trimToNull(dto.getAddress()));
        Supplier saved = supplierRepository.save(s);
        return toDto(saved);
    }

    @NotNull
    private SupplierDto toDto(Supplier s) {
        return new SupplierDto(s.getId(), s.getName(), s.getPhone(), s.getEmail(), s.getAddress());
    }

}
