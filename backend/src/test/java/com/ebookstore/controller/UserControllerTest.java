package com.ebookstore.controller;

import com.ebookstore.dto.UserDTO;
import com.ebookstore.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void getUserInfoShouldReturnSuccessAndFailureMap() {
        UserDTO userDTO = new UserDTO(1L, "Coco", "coco@example.com", "13800000000", "Shanghai");
        when(userService.getUserInfo()).thenReturn(userDTO);
        ResponseEntity<Map<String, Object>> success = userController.getUserInfo();
        assertEquals(true, success.getBody().get("success"));

        when(userService.getUserInfo()).thenThrow(new RuntimeException("not logged in"));
        ResponseEntity<Map<String, Object>> failure = userController.getUserInfo();
        assertEquals(false, failure.getBody().get("success"));
    }

    @Test
    void updateUserInfoShouldReturnSuccessAndFailureMap() {
        UserDTO userDTO = new UserDTO(1L, "Coco", "coco@example.com", "13800000000", "Shanghai");
        when(userService.updateUserInfo(userDTO)).thenReturn(userDTO);
        ResponseEntity<Map<String, Object>> success = userController.updateUserInfo(userDTO);
        assertEquals(true, success.getBody().get("success"));

        when(userService.updateUserInfo(userDTO)).thenThrow(new RuntimeException("update failed"));
        ResponseEntity<Map<String, Object>> failure = userController.updateUserInfo(userDTO);
        assertEquals(false, failure.getBody().get("success"));
    }
}
