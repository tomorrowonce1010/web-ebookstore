import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '../../contexts/AuthContext';
import authService from '../../services/authService';

jest.mock('antd', () => {
  const actual = jest.requireActual('antd');
  return {
    ...actual,
    Spin: ({ tip }) => <div>{tip || 'loading'}</div>
  };
});

jest.mock('../../services/authService', () => ({
  __esModule: true,
  default: {
    checkLoginStatus: jest.fn(),
    login: jest.fn(),
    register: jest.fn(),
    logout: jest.fn()
  }
}));

function AuthConsumer() {
  const { user, isAuthenticated, isAdmin, login, register, logout } = useAuth();

  return (
    <div>
      <div data-testid="user-name">{user ? user.username : 'anonymous'}</div>
      <div data-testid="auth-flag">{String(isAuthenticated)}</div>
      <div data-testid="admin-flag">{String(isAdmin)}</div>
      <button type="button" onClick={() => login({ username: 'coco', password: '123456' })}>
        do-login
      </button>
      <button
        type="button"
        onClick={() =>
          register({
            username: 'new-user',
            password: '123456',
            confirmPassword: '123456'
          })
        }
      >
        do-register
      </button>
      <button type="button" onClick={() => logout()}>
        do-logout
      </button>
    </div>
  );
}

describe('AuthContext', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('hydrates user from checkLoginStatus on mount', async () => {
    authService.checkLoginStatus.mockResolvedValue({
      success: true,
      userInfo: { username: 'coco', role: 'USER' }
    });

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('user-name')).toHaveTextContent('coco');
    });
    expect(screen.getByTestId('auth-flag')).toHaveTextContent('true');
    expect(screen.getByTestId('admin-flag')).toHaveTextContent('false');
  });

  test('updates user state after successful login', async () => {
    authService.checkLoginStatus.mockResolvedValue({ success: false });
    authService.login.mockResolvedValue({
      success: true,
      userInfo: { username: 'coco', role: 'USER' }
    });

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('user-name')).toHaveTextContent('anonymous');
    });

    await act(async () => {
      fireEvent.click(screen.getByText('do-login'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('user-name')).toHaveTextContent('coco');
    });
    expect(screen.getByTestId('auth-flag')).toHaveTextContent('true');
  });

  test('updates admin flag after successful register and clears state on logout', async () => {
    authService.checkLoginStatus.mockResolvedValue({ success: false });
    authService.register.mockResolvedValue({
      success: true,
      userInfo: { username: 'admin', role: 'ADMIN' }
    });
    authService.logout.mockResolvedValue({});

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('auth-flag')).toHaveTextContent('false');
    });

    await act(async () => {
      fireEvent.click(screen.getByText('do-register'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('user-name')).toHaveTextContent('admin');
    });
    expect(screen.getByTestId('admin-flag')).toHaveTextContent('true');

    await act(async () => {
      fireEvent.click(screen.getByText('do-logout'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('user-name')).toHaveTextContent('anonymous');
    });
    expect(screen.getByTestId('auth-flag')).toHaveTextContent('false');
  });
});
