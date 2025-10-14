import apiClient from './api';

// Admin list of clients (OWNER sees all; ADMIN scoped by master on backend)
export const getAdminClients = (params = {}) => {
    // Backend controller: AdminClientController @ /admin/v1/clients
    return apiClient.get('/admin/v1/clients', {params});
};
