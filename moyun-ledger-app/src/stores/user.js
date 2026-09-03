import { defineStore } from 'pinia';

const TOKEN_KEY = 'moyun_token';

/**
 * 用户会话 store（复用门户 JWT，token key 与 moyun-portal 一致）
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync(TOKEN_KEY) || '',
    userInfo: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    setToken(token) {
      this.token = token;
      uni.setStorageSync(TOKEN_KEY, token);
    },
    setUserInfo(info) {
      this.userInfo = info;
    },
    logout() {
      this.token = '';
      this.userInfo = null;
      uni.removeStorageSync(TOKEN_KEY);
    }
  }
});
