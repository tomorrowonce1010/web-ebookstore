package com.ebookstore.service.impl;

import com.ebookstore.dto.UserDTO;
import com.ebookstore.dto.UserInfoDTO;
import com.ebookstore.dto.UserListDTO;
import com.ebookstore.entity.User;
import com.ebookstore.entity.UserAuth;
import com.ebookstore.repository.UserAuthRepository;
import com.ebookstore.repository.UserRepository;
import com.ebookstore.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserAuth userAuth;

    @BeforeEach
    void setUp() {
        userAuth = new UserAuth();
        userAuth.setId(10L);
        userAuth.setUsername("coco");
        userAuth.setRole("USER");
        userAuth.setActive(true);
        userAuth.setCreatedAt(LocalDateTime.of(2026, 4, 23, 9, 0));
        userAuth.setLastLogin(LocalDateTime.of(2026, 4, 23, 10, 0));

        user = new User();
        user.setId(1L);
        user.setName("Coco");
        user.setEmail("coco@example.com");
        user.setPhone("13800000000");
        user.setAddress("Shanghai");
        user.setUserAuth(userAuth);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getAllUsersShouldConvertUserEntitiesToListDtos() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserListDTO> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("coco", result.get(0).getUsername());
        assertEquals("USER", result.get(0).getRole());
        assertTrue(result.get(0).getActive());
    }

    @Test
    void getTopSpendersCurrentlyReturnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserListDTO> result = userService.getTopSpenders("2026-04-01", "2026-04-30");

        assertEquals(1, result.size());
        assertEquals("coco", result.get(0).getUsername());
    }

    @Test
    void toggleUserStatusShouldFlipActiveFlagAndPersist() {
        ArgumentCaptor<UserAuth> authCaptor = ArgumentCaptor.forClass(UserAuth.class);
        when(userAuthRepository.findByUserId(1L)).thenReturn(Optional.of(userAuth));
        when(userAuthRepository.save(any(UserAuth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.toggleUserStatus(1L);

        verify(userAuthRepository).save(authCaptor.capture());
        assertFalse(authCaptor.getValue().getActive());
    }

    @Test
    void toggleUserStatusShouldThrowWhenUserAuthDoesNotExist() {
        when(userAuthRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.toggleUserStatus(99L));
    }

    @Test
    void getCurrentUserShouldThrowWhenNoSessionExists() {
        RequestContextHolder.resetRequestAttributes();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.getCurrentUser());

        assertTrue(exception.getMessage().contains("获取当前用户失败"));
    }

    @Test
    void getCurrentUserShouldLoadUserFromSessionInfo() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UserInfoDTO currentUserInfo = new UserInfoDTO(1L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER");
        when(authService.getCurrentUser(any(HttpSession.class))).thenReturn(currentUserInfo);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User currentUser = userService.getCurrentUser();

        assertEquals(1L, currentUser.getId());
        assertEquals("Coco", currentUser.getName());
    }

    @Test
    void getCurrentUserShouldThrowWhenAuthServiceReturnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(authService.getCurrentUser(any(HttpSession.class))).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.getCurrentUser());

        assertTrue(exception.getMessage().contains("获取当前用户失败"));
    }

    @Test
    void getCurrentUserShouldThrowWhenRepositoryCannotFindUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UserInfoDTO currentUserInfo = new UserInfoDTO(99L, "Ghost", "ghost@example.com", "Unknown", "000", "ghost", "USER");
        when(authService.getCurrentUser(any(HttpSession.class))).thenReturn(currentUserInfo);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.getCurrentUser());

        assertTrue(exception.getMessage().contains("获取当前用户失败"));
    }

    @Test
    void getUserInfoShouldReturnCurrentUserDto() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UserInfoDTO currentUserInfo = new UserInfoDTO(1L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER");
        when(authService.getCurrentUser(any(HttpSession.class))).thenReturn(currentUserInfo);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDTO result = userService.getUserInfo();

        assertEquals("Coco", result.getName());
        assertEquals("coco@example.com", result.getEmail());
    }

    @Test
    void updateUserInfoShouldPersistLatestProfile() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UserInfoDTO currentUserInfo = new UserInfoDTO(1L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER");
        UserDTO updatePayload = new UserDTO(1L, "Coco Chen", "new@example.com", "13900000000", "Beijing");

        when(authService.getCurrentUser(any(HttpSession.class))).thenReturn(currentUserInfo);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.updateUserInfo(updatePayload);

        assertEquals("Coco Chen", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("13900000000", result.getPhone());
        assertEquals("Beijing", result.getAddress());
    }

    @Test
    void getUserByIdShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getUserById(99L));
    }
}
