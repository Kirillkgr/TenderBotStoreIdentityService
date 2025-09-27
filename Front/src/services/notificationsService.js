import apiClient from './api';

export async function getUnreadCount() {
    const resp = await apiClient.get('/notifications/longpoll/unreadCount');
    return typeof resp?.data === 'number' ? resp.data : 0;
}
