import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import {useAuthStore} from '@/store/auth';
import * as authService from '@/services/authService';

const USER_STORAGE_KEY = 'user_data';

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it('hydrates user from localStorage', () => {
    const user = { id: 1, username: 'u', firstName: 'Имя', roles: ['USER'] };
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));

    const auth = useAuthStore();
    expect(auth.user).toBeNull();
    auth.hydrateFromStorage();
    expect(auth.user?.username).toBe('u');
    expect(auth.user?.firstName).toBe('Имя');
  });

  it('restoreSession success stores accessToken', async () => {
    // Mock refresh via authService (axios client under the hood)
    vi.spyOn(authService, 'refresh').mockResolvedValue({data: {accessToken: 'token123'}});
    // Mock whoami to avoid real axios call and stderr noise
    vi.spyOn(authService, 'getCurrentUser').mockResolvedValue({id: 1, username: 'u'});
    const auth = useAuthStore();
    const ok = await auth.restoreSession();
    expect(ok).toBe(true);
    expect(auth.accessToken).toBe('token123');
  });

  it('restoreSession failure clears session', async () => {
    vi.spyOn(authService, 'refresh').mockRejectedValue(new Error('no cookie'));
    const auth = useAuthStore();
    auth.setUser({ username: 'u' });
    auth.setAccessToken('a');
    const ok = await auth.restoreSession();
    expect(ok).toBe(false);
    expect(auth.user).toBeNull();
    expect(auth.accessToken).toBeNull();
  });
});
