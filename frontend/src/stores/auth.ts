import { defineStore } from 'pinia';
import {ref} from "vue";
import type {Role} from "@/types";

export const useAuthStore = defineStore('auth', () => {
    const user = ref();
    const fullName = ref("")
    const role = ref<Role>("admin")

    const hasRole = (allowedRoles: Role[]) => {
        if (!user.value) return false;
        return allowedRoles.includes(user.value.role?.toLowerCase());
    };

    function logout() {
        console.log('logout');
    }

    return {
        hasRole,
        logout,
        fullName,
        role
    }
});