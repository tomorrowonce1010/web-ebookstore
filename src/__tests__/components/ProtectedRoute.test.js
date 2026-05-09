import { render, screen, waitFor } from '@testing-library/react';
import ProtectedRoute from '../../components/ProtectedRoute';
import authService from '../../services/authService';

jest.mock('antd', () => {
  const actual = jest.requireActual('antd');
  return {
    ...actual,
    Spin: ({ tip }) => <div>{tip || 'loading'}</div>
  };
});

jest.mock('react-router-dom', () => ({
  Navigate: ({ to }) => <div>{`redirect:${to}`}</div>
}), { virtual: true });

jest.mock('../../services/authService', () => ({
  __esModule: true,
  default: {
    isAuthenticated: jest.fn(),
    getCurrentUser: jest.fn(),
    checkLoginStatus: jest.fn()
  }
}));

describe('ProtectedRoute', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('redirects to login when user is not authenticated', async () => {
    authService.isAuthenticated.mockReturnValue(false);
    authService.checkLoginStatus.mockResolvedValue({ success: false });

    render(
      <ProtectedRoute>
        <div>private-page</div>
      </ProtectedRoute>
    );

    await waitFor(() => {
      expect(screen.getByText('redirect:/login')).toBeInTheDocument();
    });
  });

  test('renders child content when user is authenticated', async () => {
    authService.isAuthenticated.mockReturnValue(true);
    authService.getCurrentUser.mockReturnValue({ username: 'coco', role: 'USER' });
    authService.checkLoginStatus.mockResolvedValue({ success: true, userInfo: { username: 'coco', role: 'USER' } });

    render(
      <ProtectedRoute>
        <div>private-page</div>
      </ProtectedRoute>
    );

    await waitFor(() => {
      expect(screen.getByText('private-page')).toBeInTheDocument();
    });
  });

  test('blocks admin route when current user is not admin', async () => {
    authService.isAuthenticated.mockReturnValue(true);
    authService.getCurrentUser.mockReturnValue({ username: 'coco', role: 'USER' });
    authService.checkLoginStatus.mockResolvedValue({ success: true, userInfo: { username: 'coco', role: 'USER' } });

    render(
      <ProtectedRoute requireAdmin>
        <div>admin-page</div>
      </ProtectedRoute>
    );

    await waitFor(() => {
      expect(screen.getByRole('heading')).toBeInTheDocument();
    });
    expect(screen.queryByText('admin-page')).not.toBeInTheDocument();
  });
});
