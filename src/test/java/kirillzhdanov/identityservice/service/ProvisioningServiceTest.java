package kirillzhdanov.identityservice.service;

import kirillzhdanov.identityservice.model.Brand;
import kirillzhdanov.identityservice.model.User;
import kirillzhdanov.identityservice.model.master.MasterAccount;
import kirillzhdanov.identityservice.model.master.UserMembership;
import kirillzhdanov.identityservice.repository.BrandRepository;
import kirillzhdanov.identityservice.repository.master.MasterAccountRepository;
import kirillzhdanov.identityservice.repository.master.UserMembershipRepository;
import kirillzhdanov.identityservice.repository.pickup.PickupPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ProvisioningServiceTest {

    private MasterAccountRepository masterAccountRepository;
    private UserMembershipRepository userMembershipRepository;
    private BrandRepository brandRepository;
    private PickupPointRepository pickupPointRepository;
    private UserService userService;

    private ProvisioningService service;

    @BeforeEach
    void setUp() {
        masterAccountRepository = mock(MasterAccountRepository.class);
        userMembershipRepository = mock(UserMembershipRepository.class);
        brandRepository = mock(BrandRepository.class);
        pickupPointRepository = mock(PickupPointRepository.class);
        userService = mock(UserService.class);
        service = new ProvisioningService(masterAccountRepository, userMembershipRepository, brandRepository, pickupPointRepository, userService);
    }

    @Test
    @DisplayName("ensureDefaultBrandAndPickup: создаёт 1 бренд и обновляет membership, повторный вызов не дублирует")
    void ensureDefaultBrandAndPickup_isIdempotent() {
        User user = User.builder().id(1L).username("u").brands(new java.util.HashSet<>()).build();
        MasterAccount master = MasterAccount.builder().id(10L).name("u").build();

        // Сначала membership без бренда
        List<UserMembership> memList = new ArrayList<>();
        UserMembership membershipWithoutBrand = UserMembership.builder().user(user).master(master).brand(null).build();
        memList.add(membershipWithoutBrand);
        when(userMembershipRepository.findByUserId(eq(1L))).thenReturn(memList);

        // Бренда с таким именем у мастера нет
        when(brandRepository.existsByNameAndMaster_Id(anyString(), eq(10L))).thenReturn(false);
        // Сохранение бренда возвращает бренд с id
        when(brandRepository.saveAndFlush(any(Brand.class))).thenAnswer(inv -> {
            Brand b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });

        // 1-й вызов: создаётся бренд и обновляется membership
        service.ensureDefaultBrandAndPickup(user, master);

        verify(brandRepository, times(1)).saveAndFlush(any(Brand.class));
        verify(pickupPointRepository, times(1)).saveAndFlush(any());
        verify(userService, times(1)).save(eq(user));
        verify(userMembershipRepository, times(1)).save(any(UserMembership.class));

        // Подготовим 2-й вызов: теперь membership уже с брендом
        UserMembership membershipWithBrand = UserMembership.builder().user(user).master(master).brand(Brand.builder().id(100L).build()).build();
        when(userMembershipRepository.findByUserId(eq(1L))).thenReturn(List.of(membershipWithBrand));

        // 2-й вызов: ничего нового не создаётся
        service.ensureDefaultBrandAndPickup(user, master);

        verify(brandRepository, times(1)).saveAndFlush(any(Brand.class));
        verify(pickupPointRepository, times(1)).saveAndFlush(any());
        // userService.save не обязателен второй раз
        verify(userService, atLeastOnce()).save(eq(user));
    }

    @Test
    @DisplayName("ensureMasterAccountForUser: создаёт мастера если нет, повторно возвращает существующего")
    void ensureMasterAccountForUser_createOnce() {
        User user = User.builder().id(1L).username("u").build();

        when(masterAccountRepository.findByName("u")).thenReturn(Optional.empty())
                .thenReturn(Optional.of(MasterAccount.builder().id(10L).name("u").build()));
        when(masterAccountRepository.save(any(MasterAccount.class))).thenAnswer(inv -> {
            MasterAccount m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        MasterAccount first = service.ensureMasterAccountForUser(user);
        MasterAccount second = service.ensureMasterAccountForUser(user);

        assertThat(first.getId()).isEqualTo(10L);
        assertThat(second.getId()).isEqualTo(10L);
        verify(masterAccountRepository, times(2)).findByName("u");
        verify(masterAccountRepository, times(1)).save(any(MasterAccount.class));
    }
}
