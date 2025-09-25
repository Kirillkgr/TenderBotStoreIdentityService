import apiClient from './api';

// Admin list of clients (OWNER sees all; ADMIN scoped by master on backend)
export const getAdminClients = (params = {}) => {
    return apiClient.get('/admin/v1/clients', {params});
};
