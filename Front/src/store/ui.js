import {defineStore} from 'pinia';

export const useUiStore = defineStore('ui', {
    state: () => ({
        // Mobile: overlay behavior uses sidebarOpen
        sidebarOpen: false,
        // Desktop: docked behavior uses collapsed rail
        isDesktop: false,
        sidebarCollapsed: (() => {
            try {
                return localStorage.getItem('ui_sidebar_collapsed') === '1';
            } catch {
                return false;
            }
        })(),
    }),
    getters: {
        isDocked(state) {
            return state.isDesktop;
        },
    },
    actions: {
        setDesktop(v) {
            this.isDesktop = !!v;
        },
        openSidebar() {
            this.sidebarOpen = true;
        },
        closeSidebar() {
            this.sidebarOpen = false;
        },
        setCollapsed(v) {
            this.sidebarCollapsed = !!v;
            try {
                localStorage.setItem('ui_sidebar_collapsed', this.sidebarCollapsed ? '1' : '0');
            } catch {
            }
        },
        toggleCollapsed() {
            this.setCollapsed(!this.sidebarCollapsed);
        },
        toggleSidebar() {
            if (this.isDesktop) {
                this.sidebarOpen = !this.sidebarOpen; // на десктопе полностью показываем/скрываем
            } else {
                this.sidebarOpen = !this.sidebarOpen; // на мобилке overlay
            }
        },
    },
});
