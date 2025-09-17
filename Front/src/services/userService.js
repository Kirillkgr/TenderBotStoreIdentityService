import apiClient from './api';

// PATCH /v1/user/edit — редактирование профиля
export const editProfile = (profileData) => {
  return apiClient.patch('/v1/user/edit', profileData);
};

// POST /v1/user/verifield/email — запросить код подтверждения на email
export const requestEmailVerification = (email) => {
  return apiClient.post('/v1/user/verifield/email', { email });
};

// PATCH /v1/user/verifield/email — отправить код подтверждения
export const verifyEmailCode = (code) => {
  return apiClient.patch('/v1/user/verifield/email', { code });
};
