package com.ebookstore.controller;

import com.ebookstore.dto.CartItemDTO;
import com.ebookstore.dto.UserInfoDTO;
import com.ebookstore.service.AuthService;
import com.ebookstore.service.BookService;
import com.ebookstore.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private AuthService authService;

    @Mock
    private BookService bookService;

    @InjectMocks
    private CartController cartController;

    private MockHttpSession session;
    private UserInfoDTO user;
    private CartItemDTO cartItem;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        user = new UserInfoDTO(1L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER");
        cartItem = new CartItemDTO();
        cartItem.setId(11L);
        cartItem.setQuantity(2);
    }

    @Test
    void getCartItemsShouldReturnUnauthorizedSuccessAndFailureBranches() {
        when(authService.getCurrentUser(session)).thenReturn(null);
        ResponseEntity<Map<String, Object>> unauthorized = cartController.getCartItems(session);
        assertEquals(HttpStatus.UNAUTHORIZED, unauthorized.getStatusCode());
        assertEquals(false, unauthorized.getBody().get("success"));

        when(authService.getCurrentUser(session)).thenReturn(user);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem));
        ResponseEntity<Map<String, Object>> success = cartController.getCartItems(session);
        assertEquals(HttpStatus.OK, success.getStatusCode());
        assertEquals(List.of(cartItem), success.getBody().get("data"));

        when(cartService.getCartItems()).thenThrow(new RuntimeException("cart broken"));
        ResponseEntity<Map<String, Object>> failure = cartController.getCartItems(session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
        assertTrue(failure.getBody().get("message").toString().contains("cart broken"));
    }

    @Test
    void addToCartShouldParsePayloadAndHandleFailure() {
        when(authService.getCurrentUser(session)).thenReturn(user);
        when(cartService.addToCart(100L, 3)).thenReturn(cartItem);

        ResponseEntity<Map<String, Object>> success = cartController.addToCart(
                Map.of("bookId", "100", "quantity", 3), session);

        assertEquals(HttpStatus.OK, success.getStatusCode());
        assertEquals(cartItem, success.getBody().get("data"));

        when(cartService.addToCart(100L, 9)).thenThrow(new RuntimeException("stock not enough"));
        ResponseEntity<Map<String, Object>> failure = cartController.addToCart(
                Map.of("bookId", 100L, "quantity", "9"), session);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
        assertEquals(false, failure.getBody().get("success"));
    }

    @Test
    void removeFromCartShouldCheckLoginAndDelegateWithUserId() {
        when(authService.getCurrentUser(session)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, cartController.removeFromCart(11L, session).getStatusCode());

        when(authService.getCurrentUser(session)).thenReturn(user);
        ResponseEntity<Map<String, Object>> success = cartController.removeFromCart(11L, session);
        assertEquals(HttpStatus.OK, success.getStatusCode());
        verify(cartService).removeFromCart(11L, 1L);

        doThrow(new RuntimeException("remove failed")).when(cartService).removeFromCart(12L, 1L);
        ResponseEntity<Map<String, Object>> failure = cartController.removeFromCart(12L, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
    }

    @Test
    void updateCartItemQuantityShouldParsePayloadAndHandleFailure() {
        when(authService.getCurrentUser(session)).thenReturn(user);

        ResponseEntity<Map<String, Object>> success = cartController.updateCartItemQuantity(
                11L, Map.of("quantity", "5"), session);
        assertEquals(HttpStatus.OK, success.getStatusCode());
        verify(cartService).updateCartItemQuantity(11L, 5, 1L);

        doThrow(new RuntimeException("update failed")).when(cartService).updateCartItemQuantity(12L, 7, 1L);
        ResponseEntity<Map<String, Object>> failure = cartController.updateCartItemQuantity(
                12L, Map.of("quantity", 7), session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
    }
}
