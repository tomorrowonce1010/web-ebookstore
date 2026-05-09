package com.ebookstore.service.impl;

import com.ebookstore.dao.UserAuthDao;
import com.ebookstore.dao.UserDao;
import com.ebookstore.dto.LoginDTO;
import com.ebookstore.dto.LoginResponseDTO;
import com.ebookstore.dto.RegisterDTO;
import com.ebookstore.dto.UserInfoDTO;
import com.ebookstore.entity.User;
import com.ebookstore.entity.UserAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserAuthDao userAuthDao;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginDTO loginDTO;
    private RegisterDTO registerDTO;

    @BeforeEach
    void setUp() {
        loginDTO = new LoginDTO();
        loginDTO.setUsername("coco");
        loginDTO.setPassword("123456");

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("new-user");
        registerDTO.setPassword("123456");
        registerDTO.setConfirmPassword("123456");
        registerDTO.setName("Coco");
        registerDTO.setEmail("coco@example.com");
        registerDTO.setAddress("Shanghai");
        registerDTO.setPhone("13800000000");
    }

    @Test
    void loginShouldFailWhenUserDoesNotExist() {
        MockHttpSession session = new MockHttpSession();
        when(userAuthDao.findByUsername("coco")).thenReturn(Optional.empty());

        LoginResponseDTO response = authService.login(loginDTO, session);

        assertFalse(response.getSuccess());
        assertNull(response.getUserInfo());
        assertNull(session.getAttribute("currentUser"));
        verify(userAuthDao, never()).save(any(UserAuth.class));
    }

    @Test
    void loginShouldFailWhenPasswordDoesNotMatch() {
        MockHttpSession session = new MockHttpSession();
        UserAuth userAuth = buildUserAuth(1L, "coco", "USER", true);
        userAuth.setPasswordHash(authService.encodePassword("wrong-password"));
        when(userAuthDao.findByUsername("coco")).thenReturn(Optional.of(userAuth));

        LoginResponseDTO response = authService.login(loginDTO, session);

        assertFalse(response.getSuccess());
        assertNull(session.getAttribute("currentUser"));
        verify(userAuthDao, never()).save(any(UserAuth.class));
    }

    @Test
    void loginShouldFailWhenUserIsInactive() {
        MockHttpSession session = new MockHttpSession();
        UserAuth userAuth = buildUserAuth(1L, "coco", "USER", false);
        userAuth.setPasswordHash(authService.encodePassword("123456"));
        when(userAuthDao.findByUsername("coco")).thenReturn(Optional.of(userAuth));

        LoginResponseDTO response = authService.login(loginDTO, session);

        assertFalse(response.getSuccess());
        assertNull(session.getAttribute("currentUser"));
        verify(userAuthDao, never()).save(any(UserAuth.class));
    }

    @Test
    void loginShouldStoreCurrentUserInSessionWhenSuccessful() {
        MockHttpSession session = new MockHttpSession();
        UserAuth userAuth = buildUserAuth(1L, "coco", "USER", true);
        userAuth.setPasswordHash(authService.encodePassword("123456"));
        User user = buildUser(10L, "Coco", "coco@example.com", userAuth);

        when(userAuthDao.findByUsername("coco")).thenReturn(Optional.of(userAuth));
        when(userAuthDao.save(any(UserAuth.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDao.findByUserAuth(userAuth)).thenReturn(Optional.of(user));

        LoginResponseDTO response = authService.login(loginDTO, session);

        assertTrue(response.getSuccess());
        assertNotNull(response.getUserInfo());
        assertEquals("coco", response.getUserInfo().getUsername());
        assertNotNull(userAuth.getLastLogin());

        Object sessionUser = session.getAttribute("currentUser");
        assertInstanceOf(UserInfoDTO.class, sessionUser);
        assertEquals("coco", ((UserInfoDTO) sessionUser).getUsername());
        verify(userAuthDao).save(userAuth);
    }

    @Test
    void loginShouldStillSucceedWhenUserProfileIsMissing() {
        MockHttpSession session = new MockHttpSession();
        UserAuth userAuth = buildUserAuth(1L, "coco", "USER", true);
        userAuth.setPasswordHash(authService.encodePassword("123456"));

        when(userAuthDao.findByUsername("coco")).thenReturn(Optional.of(userAuth));
        when(userAuthDao.save(any(UserAuth.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDao.findByUserAuth(userAuth)).thenReturn(Optional.empty());

        LoginResponseDTO response = authService.login(loginDTO, session);

        assertTrue(response.getSuccess());
        assertEquals("coco", response.getUserInfo().getUsername());
        assertNull(response.getUserInfo().getId());
    }

    @Test
    void loginShouldReturnFailureWhenDaoThrowsException() {
        MockHttpSession session = new MockHttpSession();
        when(userAuthDao.findByUsername("coco")).thenThrow(new RuntimeException("database unavailable"));

        LoginResponseDTO response = authService.login(loginDTO, session);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("database unavailable"));
        assertNull(session.getAttribute("currentUser"));
    }

    @Test
    void registerShouldFailWhenPasswordsDoNotMatch() {
        registerDTO.setConfirmPassword("654321");

        LoginResponseDTO response = authService.register(registerDTO);

        assertFalse(response.getSuccess());
        verify(userAuthDao, never()).save(any(UserAuth.class));
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void registerShouldFailWhenUsernameAlreadyExists() {
        when(userAuthDao.existsByUsername("new-user")).thenReturn(true);

        LoginResponseDTO response = authService.register(registerDTO);

        assertFalse(response.getSuccess());
        verify(userAuthDao, never()).save(any(UserAuth.class));
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void registerShouldFailWhenEmailAlreadyExists() {
        when(userAuthDao.existsByUsername("new-user")).thenReturn(false);
        when(userDao.existsByEmail("coco@example.com")).thenReturn(true);

        LoginResponseDTO response = authService.register(registerDTO);

        assertFalse(response.getSuccess());
        verify(userAuthDao, never()).save(any(UserAuth.class));
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void registerShouldReturnFailureWhenPersistenceThrowsException() {
        when(userAuthDao.existsByUsername("new-user")).thenReturn(false);
        when(userDao.existsByEmail("coco@example.com")).thenReturn(false);
        when(userAuthDao.save(any(UserAuth.class))).thenThrow(new RuntimeException("write failed"));

        LoginResponseDTO response = authService.register(registerDTO);

        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("write failed"));
    }

    @Test
    void registerShouldPersistUserAndAuthWhenInputIsValid() {
        ArgumentCaptor<UserAuth> authCaptor = ArgumentCaptor.forClass(UserAuth.class);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userAuthDao.existsByUsername("new-user")).thenReturn(false);
        when(userDao.existsByEmail("coco@example.com")).thenReturn(false);
        when(userAuthDao.save(any(UserAuth.class))).thenAnswer(invocation -> {
            UserAuth saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });
        when(userDao.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(30L);
            return saved;
        });

        LoginResponseDTO response = authService.register(registerDTO);

        assertTrue(response.getSuccess());
        assertNotNull(response.getUserInfo());
        assertEquals("new-user", response.getUserInfo().getUsername());
        assertEquals("Coco", response.getUserInfo().getName());

        verify(userAuthDao).save(authCaptor.capture());
        verify(userDao).save(userCaptor.capture());

        UserAuth savedAuth = authCaptor.getValue();
        User savedUser = userCaptor.getValue();
        assertEquals("new-user", savedAuth.getUsername());
        assertEquals("USER", savedAuth.getRole());
        assertTrue(savedAuth.getActive());
        assertTrue(authService.validatePassword("123456", savedAuth.getPasswordHash()));

        assertEquals("Coco", savedUser.getName());
        assertEquals("coco@example.com", savedUser.getEmail());
        assertSame(savedAuth, savedUser.getUserAuth());
    }

    @Test
    void logoutShouldRemoveCurrentUserAndInvalidateSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", new UserInfoDTO());

        authService.logout(session);

        assertTrue(session.isInvalid());
    }

    @Test
    void getCurrentUserShouldReadUserInfoFromSession() {
        MockHttpSession session = new MockHttpSession();
        UserInfoDTO userInfo = new UserInfoDTO(1L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER");
        session.setAttribute("currentUser", userInfo);

        UserInfoDTO result = authService.getCurrentUser(session);

        assertSame(userInfo, result);
    }

    private UserAuth buildUserAuth(Long id, String username, String role, boolean active) {
        UserAuth userAuth = new UserAuth();
        userAuth.setId(id);
        userAuth.setUsername(username);
        userAuth.setRole(role);
        userAuth.setActive(active);
        return userAuth;
    }

    private User buildUser(Long id, String name, String email, UserAuth userAuth) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setAddress("Shanghai");
        user.setPhone("13800000000");
        user.setUserAuth(userAuth);
        return user;
    }
}
