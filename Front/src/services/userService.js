import apiClient from './api';

// PATCH /user/v1/edit — редактирование профиля
export const editProfile = (profileData) => {
  return apiClient.patch('/user/v1/edit', profileData);
};

// POST /user/v1/email/verified — проверить подтверждён ли email (возвращает { verified: boolean } или boolean)
export const checkEmailVerified = (email) => {
  return apiClient.post('/user/v1/email/verified', { email });
};

// POST /user/v1/verifield/email — запросить код подтверждения на email
export const requestEmailVerification = (email) => {
  return apiClient.post('/user/v1/verifield/email', { email });
};

// PATCH /user/v1/verifield/email — отправить код подтверждения
export const verifyEmailCode = (email, code) => {
  return apiClient.patch('/user/v1/verifield/email', { email, code });
};
