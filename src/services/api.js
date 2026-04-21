// API基础URL
import API_BASE_URL from './config';

// 通用请求处理函数
async function request(url, options = {}) {
  try {
    const defaultOptions = {
      credentials: 'include', // 包含cookies
      headers: {
        'Content-Type': 'application/json',
      },
    };

    const response = await fetch(`${API_BASE_URL}${url}`, {
      ...defaultOptions,
      ...options,
    });

    // 检查响应状态
    if (!response.ok) {
      const errorText = await response.text();
      console.error(`API请求失败: ${response.status} ${response.statusText}`, errorText);
      throw new Error(`HTTP ${response.status}: ${errorText}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error('API请求失败:', error);
    throw error;
  }
}

// 用户相关API
export const userApi = {
  // 登录
  login: (credentials) => request('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  }),

  // 注册
  register: (userData) => request('/auth/register', {
    method: 'POST',
    body: JSON.stringify(userData),
  }),

  // 登出
  logout: () => request('/auth/logout', {
    method: 'POST',
  }),

  // 获取当前用户信息
  getCurrentUser: () => request('/auth/current-user'),

  // 获取用户详细信息
  getUserInfo: () => request('/user/info'),

  // 更新用户信息
  updateUserInfo: (userData) => request('/user/update', {
    method: 'PUT',
    body: JSON.stringify(userData),
  }),
};

// 书籍相关API
export const bookApi = {
  // 获取所有书籍
  getBooks: () => request('/books'),
  
  // 获取单本书籍详情
  getBook: (id) => request(`/books/${id}`),
  
  // 搜索书籍
  searchBooks: (query) => request(`/books/search?query=${encodeURIComponent(query)}`),
};

// 购物车相关API
export const cartApi = {
  // 获取购物车
  getCart: () => request('/cart'),

  // 添加到购物车
  addToCart: (item) => request('/cart/add', {
    method: 'POST',
    body: JSON.stringify(item),
  }),

  // 从购物车移除
  removeFromCart: (cartItemId) => request(`/cart/remove/${cartItemId}`, {
    method: 'DELETE',
  }),

  // 更新购物车商品数量
  updateCartItemQuantity: (cartItemId, quantity) => request(`/cart/update/${cartItemId}`, {
    method: 'PUT',
    body: JSON.stringify({ quantity }),
  }),
};

// 订单相关API
export const orderApi = {
  // 获取订单列表
  getOrders: () => request('/orders'),

  // 获取订单详情
  getOrder: (orderId) => request(`/orders/${orderId}`),

  // 搜索用户订单
  searchOrders: (params) => {
    const queryParams = new URLSearchParams();
    if (params.bookName) queryParams.append('bookName', params.bookName);
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);
    
    return request(`/orders/search?${queryParams.toString()}`);
  },

  // 管理员获取所有订单
  getAllOrdersForAdmin: () => request('/orders/admin/all'),

  // 管理员搜索所有订单
  searchAllOrdersForAdmin: (params) => {
    const queryParams = new URLSearchParams();
    if (params.bookName) queryParams.append('bookName', params.bookName);
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);
    
    return request(`/orders/admin/search?${queryParams.toString()}`);
  },

  // 创建订单
  createOrder: (orderData) => request('/orders/create', {
    method: 'POST',
    body: JSON.stringify(orderData),
  }),

  // 取消订单
  cancelOrder: (orderId) => request(`/orders/${orderId}/cancel`, {
    method: 'POST',
  }),
};

// 管理员用户管理API
export const getAllUsers = () => request('/admin/users');
//改变用户状态api调用封装
export const toggleUserStatus = (userId) => request(`/admin/users/${userId}/status`, {
  method: 'PUT',
});

export const getUserStatistics = (startDate, endDate) => request(`/admin/users/statistics?startDate=${startDate}&endDate=${endDate}`);

// 管理员书籍管理API
export const adminBookApi = {
  // 获取所有书籍（分页）
  getBooks: (page = 0, size = 10) => request(`/admin/books?page=${page}&size=${size}`),
  
  // 获取书籍详情
  getBook: (id) => request(`/admin/books/${id}`),
  
  // 添加书籍
  addBook: (bookData) => request('/admin/books', {
    method: 'POST',
    body: JSON.stringify(bookData),
  }),
  
  // 更新书籍
  updateBook: (id, bookData) => request(`/admin/books/${id}`, {
    method: 'PUT',
    body: JSON.stringify(bookData),
  }),
  
  // 删除书籍
  deleteBook: (id) => request(`/admin/books/${id}`, {
    method: 'DELETE',
  }),
  
  // 搜索书籍
  searchBooks: (keyword) => request(`/admin/books/search?keyword=${encodeURIComponent(keyword)}`),
  
  // 更新书籍库存
  updateStock: (id, stock) => request(`/admin/books/${id}/stock?stock=${stock}`, {
    method: 'PUT',
  }),
  
  // 检查书籍库存
  checkStock: (id, quantity = 1) => request(`/admin/books/${id}/stock?quantity=${quantity}`),
};

// 统计相关API
export const statisticsApi = {
  // 获取书籍销量统计（热销榜）- 管理员功能
  getBookSalesStatistics: (startDate, endDate) => {
    const params = new URLSearchParams();
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    return request(`/statistics/books?${params.toString()}`);
  },

  // 获取用户消费统计（消费榜）- 管理员功能
  getUserConsumptionStatistics: (startDate, endDate) => {
    const params = new URLSearchParams();
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    return request(`/statistics/users?${params.toString()}`);
  },

  // 获取个人购书统计 - 用户功能
  getPersonalStatistics: (startDate, endDate) => {
    const params = new URLSearchParams();
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    return request(`/statistics/personal?${params.toString()}`);
  },
}; 
