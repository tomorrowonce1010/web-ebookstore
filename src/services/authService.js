import API_BASE_URL from './config';

const REQUEST_TIMEOUT = 5000;

class AuthService {
  async fetchWithTimeout(url, options = {}) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT);

    try {
      return await fetch(url, {
        ...options,
        signal: controller.signal
      });
    } finally {
      clearTimeout(timeoutId);
    }
  }

  async login(loginData) {
    try {
      const response = await this.fetchWithTimeout(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(loginData)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const result = await response.json();

      if (result.success) {
        localStorage.setItem('userInfo', JSON.stringify(result.userInfo));
        localStorage.setItem('isLoggedIn', 'true');
        return { success: true, userInfo: result.userInfo };
      }

      return { success: false, message: result.message };
    } catch (error) {
      console.error('登录请求失败:', error);
      return {
        success: false,
        message:
          error.name === 'AbortError'
            ? '请求超时，请确认后端服务已启动'
            : error.message || '网络错误，请稍后重试'
      };
    }
  }

  async register(registerData) {
    try {
      const response = await this.fetchWithTimeout(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(registerData)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const result = await response.json();

      if (result.success) {
        localStorage.setItem('userInfo', JSON.stringify(result.userInfo));
        localStorage.setItem('isLoggedIn', 'true');
        return { success: true, userInfo: result.userInfo };
      }

      return { success: false, message: result.message };
    } catch (error) {
      console.error('注册请求失败:', error);
      return {
        success: false,
        message:
          error.name === 'AbortError'
            ? '请求超时，请确认后端服务已启动'
            : error.message || '网络错误，请稍后重试'
      };
    }
  }

  async logout() {
    try {
      await this.fetchWithTimeout(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        credentials: 'include'
      });
    } catch (error) {
      console.error('登出请求失败:', error);
    } finally {
      localStorage.removeItem('userInfo');
      localStorage.removeItem('isLoggedIn');
      window.location.href = '/login';
    }
  }

  async checkLoginStatus() {
    try {
      const response = await this.fetchWithTimeout(`${API_BASE_URL}/auth/status`, {
        method: 'GET',
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();

      if (result.isLoggedIn) {
        localStorage.setItem('userInfo', JSON.stringify(result.userInfo));
        localStorage.setItem('isLoggedIn', 'true');
        return { success: true, userInfo: result.userInfo };
      }

      localStorage.removeItem('userInfo');
      localStorage.removeItem('isLoggedIn');
      return { success: false };
    } catch (error) {
      console.error('检查登录状态失败:', error);
      const isLoggedIn = localStorage.getItem('isLoggedIn');
      const userInfo = localStorage.getItem('userInfo');

      if (isLoggedIn && userInfo) {
        return { success: true, userInfo: JSON.parse(userInfo) };
      }

      return { success: false };
    }
  }

  getCurrentUser() {
    const userInfo = localStorage.getItem('userInfo');
    const isLoggedIn = localStorage.getItem('isLoggedIn');

    if (isLoggedIn && userInfo) {
      return JSON.parse(userInfo);
    }

    return null;
  }

  setCurrentUser(userInfo) {
    if (userInfo) {
      localStorage.setItem('userInfo', JSON.stringify(userInfo));
      localStorage.setItem('isLoggedIn', 'true');
    } else {
      localStorage.removeItem('userInfo');
      localStorage.removeItem('isLoggedIn');
    }
  }

  isAuthenticated() {
    return localStorage.getItem('isLoggedIn') === 'true';
  }

  isAdmin() {
    const userInfo = this.getCurrentUser();
    return userInfo && userInfo.role === 'ADMIN';
  }
}

const authService = new AuthService();

export default authService;
