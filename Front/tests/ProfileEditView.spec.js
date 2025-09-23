import {beforeEach, describe, expect, it, vi} from 'vitest';
import {mount} from '@vue/test-utils';
import {createPinia, setActivePinia} from 'pinia';
import ProfileEditView from '@/views/ProfileEditView.vue';
import {useAuthStore} from '@/store/auth';

// Stub ModalBase to ensure slot content is rendered in tests
vi.mock('@/components/ui/ModalBase.vue', () => ({
  default: {
    name: 'ModalBase',
    props: ['modelValue', 'title', 'width'],
    emits: ['update:modelValue'],
    template: '<div class="modal-stub"><slot /></div>'
  }
}));

// Mock router
vi.mock('vue-router', () => {
  const routerStub = {
    currentRoute: { value: { fullPath: '/' } },
    beforeEach: vi.fn(),
    afterEach: vi.fn(),
    replace: vi.fn(async () => {}),
    push: vi.fn(async () => {}),
    back: vi.fn(),
  };
  return {
    useRouter: () => routerStub,
    createRouter: () => routerStub,
    createWebHistory: () => ({}),
  };
});

// Mock UserApi
vi.mock('@/services/user', () => ({
  UserApi: {
    editProfile: vi.fn(async (payload) => ({
      lastName: payload.lastName,
      firstName: payload.firstName,
      patronymic: payload.patronymic ?? null,
      email: payload.email,
      phone: payload.phone,
      dateOfBirth: payload.dateOfBirth ?? undefined,
    }))
  }
}));

// Stub toast
vi.mock('vue-toastification', () => ({ useToast: () => ({ success: vi.fn(), error: vi.fn() }) }));

const USER_STORAGE_KEY = 'user_data';

describe('ProfileEditView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it('prefills form from localStorage user_data', async () => {
    const user = {
      id: 1,
      username: 'user1',
      firstName: 'Имя',
      lastName: 'Фамилия',
      patronymic: 'Отчество',
      dateOfBirth: '1990-01-02T00:00:00Z',
      email: 'test@example.com',
      phone: '+71234567890',
      roles: ['USER']
    };
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));

    const auth = useAuthStore();
    auth.hydrateFromStorage();

    const wrapper = mount(ProfileEditView, { attachTo: document.body });

    const inputs = wrapper.findAll('input');
    // Order per template: lastName, firstName, patronymic, birthDate, email, phone, password, confirmPassword
    expect(inputs[0].element.value).toBe('Фамилия');
    expect(inputs[1].element.value).toBe('Имя');
    expect(inputs[2].element.value).toBe('Отчество');
    expect(inputs[3].element.value).toBe('1990-01-02');
    expect(inputs[4].element.value).toBe('test@example.com');
    expect(inputs[5].element.value).toBe('+71234567890');
  });

  it('submits edited data and updates auth store', async () => {
    const user = {
      id: 1,
      username: 'user1',
      firstName: 'Имя',
      lastName: 'Фамилия',
      patronymic: 'Отчество',
      dateOfBirth: '1990-01-02',
      email: 'test@example.com',
      phone: '+71234567890',
      roles: ['USER']
    };
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));

    const auth = useAuthStore();
    auth.hydrateFromStorage();

    const wrapper = mount(ProfileEditView, { attachTo: document.body });

    // Change some fields
    await wrapper.find('input[placeholder="Имя"]').setValue('НовыйИмя');
    await wrapper.find('input[placeholder="email@email.ru"]').setValue('new@example.com');

    // Submit
    await wrapper.find('form').trigger('submit.prevent');

    // auth.user should be updated
    expect(auth.user.firstName).toBe('НовыйИмя');
    expect(auth.user.email).toBe('new@example.com');
  });
});
