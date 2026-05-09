package com.ebookstore.controller;

import com.ebookstore.dto.LoginDTO;
import com.ebookstore.dto.LoginResponseDTO;
import com.ebookstore.dto.RegisterDTO;
import com.ebookstore.dto.UserInfoDTO;
import com.ebookstore.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockHttpSession session;
    private UserInfoDTO userInfo;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        userInfo = new UserInfoDTO(1L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER");
    }

    @Test
    void loginShouldReturnServiceResponseForSuccessAndFailure() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("coco");
        loginDTO.setPassword("123456");

        when(authService.login(loginDTO, session)).thenReturn(LoginResponseDTO.success(userInfo));
        ResponseEntity<LoginResponseDTO> success = authController.login(loginDTO, session);
        assertTrue(success.getBody().getSuccess());

        when(authService.login(loginDTO, session)).thenReturn(LoginResponseDTO.failure("密码错误"));
        ResponseEntity<LoginResponseDTO> failure = authController.login(loginDTO, session);
        assertFalse(failure.getBody().getSuccess());
    }

    @Test
    void registerShouldReturnServiceResponse() {
        RegisterDTO registerDTO = new RegisterDTO();
        when(authService.register(registerDTO)).thenReturn(LoginResponseDTO.success(userInfo));

        ResponseEntity<LoginResponseDTO> response = authController.register(registerDTO);

        assertTrue(response.getBody().getSuccess());
        assertEquals("coco", response.getBody().getUserInfo().getUsername());
    }

    @Test
    void logoutShouldCallServiceAndReturnSuccessMap() {
        ResponseEntity<Map<String, Object>> response = authController.logout(session);

        assertTrue((Boolean) response.getBody().get("success"));
        verify(authService).logout(session);
    }

    @Test
    void checkLoginStatusShouldReturnLoggedInStateAndAnonymousState() {
        when(authService.getCurrentUser(session)).thenReturn(userInfo);
        ResponseEntity<Map<String, Object>> loggedIn = authController.checkLoginStatus(session);
        assertEquals(true, loggedIn.getBody().get("isLoggedIn"));
        assertSame(userInfo, loggedIn.getBody().get("userInfo"));

        when(authService.getCurrentUser(session)).thenReturn(null);
        ResponseEntity<Map<String, Object>> anonymous = authController.checkLoginStatus(session);
        assertEquals(false, anonymous.getBody().get("isLoggedIn"));
        assertNull(anonymous.getBody().get("userInfo"));
    }

    @Test
    void getCurrentUserShouldReturnSuccessOrFailureMap() {
        when(authService.getCurrentUser(session)).thenReturn(userInfo);
        ResponseEntity<Map<String, Object>> success = authController.getCurrentUser(session);
        assertEquals(true, success.getBody().get("success"));
        assertSame(userInfo, success.getBody().get("userInfo"));

        when(authService.getCurrentUser(session)).thenReturn(null);
        ResponseEntity<Map<String, Object>> failure = authController.getCurrentUser(session);
        assertEquals(false, failure.getBody().get("success"));
        assertNotNull(failure.getBody().get("message"));
    }
}
