import api from './api';

export const StaffApi = {
  // Users
  listUsers(params) {
    return api.get('/staff/v1/users', { params }).then(r => r.data);
  },
  createUser(payload) {
    return api.post('/staff/v1/users', payload).then(r => r.data);
  },
  updateUser(id, payload) {
    return api.put(`/staff/v1/users/${id}`, payload).then(r => r.data);
  },
  deleteUser(id) {
    return api.delete(`/staff/v1/users/${id}`).then(r => r.data);
  },

  // Departments
  listDepartments() {
    return api.get('/staff/v1/departments').then(r => r.data);
  },
  createDepartment(payload) {
    return api.post('/staff/v1/departments', payload).then(r => r.data);
  },
};
