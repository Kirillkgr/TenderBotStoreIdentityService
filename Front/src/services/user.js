import api from './api';

export const UserApi = {
  editProfile(payload) {
    return api.patch('/user/v1/edit', payload).then(r => r.data);
  },
  checkEmailVerified(email) {
    return api.post('/user/v1/email/verified', { email }).then(r => r.data);
  },
  requestEmailCode(email) {
    return api.post('/user/v1/verifield/email', { email }).then(r => r.data);
  },
  verifyEmailCode(email, code) {
    return api.patch('/user/v1/verifield/email', { email, code }).then(r => r.data);
  },
};
