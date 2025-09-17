import {createApp} from 'vue';
import {createPinia} from 'pinia';
import Toast from 'vue-toastification';
import 'vue-toastification/dist/index.css';
import 'vue-datepicker-next/index.css';

import App from './App.vue';
import router from './router';
import './style.css';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(Toast, {
    // Опции для уведомлений, если нужно
    transition: "Vue-Toastification__bounce",
    maxToasts: 5,
    newestOnTop: true
});

app.mount('#app');