import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('react-router-dom', () => ({
  BrowserRouter: ({ children }) => <>{children}</>,
  Routes: ({ children }) => <>{children}</>,
  Route: ({ element }) => element
}), { virtual: true });

jest.mock('./contexts/AuthContext', () => ({
  AuthProvider: ({ children }) => <>{children}</>
}));

jest.mock('./contexts/CartContext', () => ({
  CartProvider: ({ children }) => <>{children}</>
}));

jest.mock('./components/Layout', () => ({
  __esModule: true,
  default: ({ children }) => <div data-testid="layout-shell">{children}</div>
}));

jest.mock('./components/ProtectedRoute', () => ({
  __esModule: true,
  default: ({ children }) => <>{children}</>
}));

jest.mock('./pages/LoginPage', () => () => <div>login-page</div>);
jest.mock('./pages/Home', () => () => <div>home-page</div>);
jest.mock('./pages/BookDetailPage', () => () => <div>book-detail-page</div>);
jest.mock('./pages/CartPage', () => () => <div>cart-page</div>);
jest.mock('./pages/PersonalPage', () => () => <div>personal-page</div>);
jest.mock('./pages/OrderPage', () => () => <div>order-page</div>);
jest.mock('./pages/AdminBookPage', () => () => <div>admin-book-page</div>);
jest.mock('./pages/AdminUserPage', () => () => <div>admin-user-page</div>);
jest.mock('./pages/AdminOrderPage', () => () => <div>admin-order-page</div>);
jest.mock('./pages/AdminStatisticsPage', () => () => <div>admin-statistics-page</div>);
jest.mock('./pages/PersonalStatisticsPage', () => () => <div>personal-statistics-page</div>);

test('renders application shell with mocked routes', () => {
  window.history.pushState({}, '', '/');

  render(<App />);

  expect(screen.getByText('login-page')).toBeInTheDocument();
  expect(screen.getAllByTestId('layout-shell').length).toBeGreaterThan(0);
});
