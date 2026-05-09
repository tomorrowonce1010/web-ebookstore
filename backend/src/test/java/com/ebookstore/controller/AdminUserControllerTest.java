package com.ebookstore.controller;

import com.ebookstore.dto.UserListDTO;
import com.ebookstore.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @Test
    void shouldDelegateUserManagementRequestsToService() {
        AdminUserController controller = new AdminUserController(userService);
        UserListDTO dto = new UserListDTO();
        dto.setUsername("coco");
        when(userService.getAllUsers()).thenReturn(List.of(dto));
        when(userService.getTopSpenders("2026-04-01", "2026-04-30")).thenReturn(List.of(dto));

        ResponseEntity<List<UserListDTO>> users = controller.getAllUsers();
        assertEquals(1, users.getBody().size());

        ResponseEntity<Map<String, Object>> status = controller.toggleUserStatus(1L);
        assertEquals(true, status.getBody().get("success"));
        verify(userService).toggleUserStatus(1L);

        ResponseEntity<List<UserListDTO>> topSpenders = controller.getTopSpenders("2026-04-01", "2026-04-30");
        assertEquals(1, topSpenders.getBody().size());
    }

    @Test
    void toggleUserStatusShouldReturnFailureMapWhenServiceThrows() {
        AdminUserController controller = new AdminUserController(userService);
        doThrow(new RuntimeException("missing")).when(userService).toggleUserStatus(99L);

        ResponseEntity<Map<String, Object>> response = controller.toggleUserStatus(99L);

        assertEquals(false, response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("missing"));
    }
}
