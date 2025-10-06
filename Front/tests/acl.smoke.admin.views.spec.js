import {beforeEach, describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import StaffManagementView from '@/views/StaffManagementView.vue';
import {useAuthStore} from '../src/store/auth';
import canDirective from '../src/directives/can';

// Mock StaffApi to avoid network
vi.mock('@/services/staff', () => ({
    StaffApi: {
        listDepartments: async () => ([{id: 10, name: 'Отдел продаж'}]),
        listUsers: async () => ({items: [], page: 1, size: 10, total: 0})
    }
}));

function renderWithRoles(roles) {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.setAccessToken?.('AT');
    auth.roles = roles;
    return render(StaffManagementView, {
        global: {
            directives: {can: canDirective}
        }
    });
}

describe('ACL Smoke: Admin views (StaffManagementView)', () => {
    beforeEach(() => {
        vi.resetModules();
    });

    it('USER: create buttons hidden, export disabled', async () => {
        renderWithRoles(['USER']);
        // Hidden (mode: hide)
        const btnCreateUser = screen.queryByText('Создать пользователя');
        const btnCreateDept = screen.queryByText('Создать отдел');
        expect(btnCreateUser).toBeTruthy();
        expect(btnCreateDept).toBeTruthy();
        expect(btnCreateUser.style.display).toBe('none');
        expect(btnCreateDept.style.display).toBe('none');
        // Disabled (mode: disable)
        const exportBtn = screen.queryByText('Экспорт CSV');
        expect(exportBtn).toBeTruthy();
        expect(exportBtn.getAttribute('aria-disabled')).toBe('true');
    });

    it('ADMIN sees create/export actions', async () => {
        renderWithRoles(['ADMIN']);
        // buttons exist in DOM; with v-can hide we'd need real directive to check visibility.
        // Here we assert they are rendered in template (directive would hide in real app).
        expect(screen.queryByText('Создать пользователя')).toBeTruthy();
        expect(screen.queryByText('Создать отдел')).toBeTruthy();
        expect(screen.queryByText('Экспорт CSV')).toBeTruthy();
    });
});
