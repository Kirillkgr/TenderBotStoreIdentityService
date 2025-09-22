import apiClient from './api';

// Регистрация нового пользователя
export const register = (credentials) => {
    return apiClient.post('/auth/v1/register', credentials);
};

// Вход пользователя
export const login = (credentials) => {
    const { username, password } = credentials;
    const basicAuth = 'Basic ' + btoa(`${username}:${password}`);

    // Отправляем POST-запрос с заголовком Basic Auth.
    // Тело запроса пустое, так как данные передаются в заголовке.
    return apiClient.post('/auth/v1/login', null, {
        headers: {
            'Authorization': basicAuth,
        },
    });
};

// Проверка доступности логина
export const checkUsername = (username) => {
    return apiClient.post(`/auth/v1/checkUsername?username=${username}`);
};

// Выход пользователя. Запрос инвалидирует refresh_token на бэкенде.
export const logout = (token) => {
    // Бэкенд ожидает DELETE /auth/v1/logout с body = token
    return apiClient.delete('/auth/v1/logout', { data: token });
};

// Получение информации о текущем пользователе
export const getCurrentUser = async () => {
    try {
        const response = await apiClient.get('/auth/v1/whoami');
        return response.data;
    } catch (error) {
        console.error('Ошибка при получении данных пользователя:', error);
        throw error;
    }
};
