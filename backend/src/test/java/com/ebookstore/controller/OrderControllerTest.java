package com.ebookstore.controller;

import com.ebookstore.dto.OrderDTO;
import com.ebookstore.dto.OrderItemDTO;
import com.ebookstore.dto.UserInfoDTO;
import com.ebookstore.service.AuthService;
import com.ebookstore.service.CartService;
import com.ebookstore.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private OrderController orderController;

    private MockHttpSession session;
    private UserInfoDTO user;
    private UserInfoDTO admin;
    private OrderItemDTO item;
    private OrderDTO order;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        user = new UserInfoDTO(1L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER");
        admin = new UserInfoDTO(2L, "Admin", "admin@example.com", "Shanghai", "13900000000", "admin", "ADMIN");
        item = new OrderItemDTO(10L, 100L, "Three Body", "Liu Cixin",
                new BigDecimal("88.00"), 2, new BigDecimal("176.00"), "2026-04-23");
        order = new OrderDTO(1L, LocalDateTime.of(2026, 4, 23, 10, 0),
                new BigDecimal("176.00"), "COMPLETED", List.of(item));
    }

    @Test
    void getOrdersShouldHandleUnauthorizedSuccessAndFailure() {
        when(authService.getCurrentUser(session)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, orderController.getOrders(session).getStatusCode());

        when(authService.getCurrentUser(session)).thenReturn(user);
        when(orderService.getOrders()).thenReturn(List.of(item));
        ResponseEntity<Map<String, Object>> success = orderController.getOrders(session);
        assertEquals(HttpStatus.OK, success.getStatusCode());
        assertEquals(List.of(item), success.getBody().get("data"));

        when(orderService.getOrders()).thenThrow(new RuntimeException("order failed"));
        ResponseEntity<Map<String, Object>> failure = orderController.getOrders(session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
    }

    @Test
    void searchOrdersShouldParseDateRangeAndHandleBadInput() {
        when(authService.getCurrentUser(session)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED,
                orderController.searchOrders("three", null, null, session).getStatusCode());

        when(authService.getCurrentUser(session)).thenReturn(user);
        when(orderService.searchUserOrders(eq("three"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(order));

        ResponseEntity<Map<String, Object>> success = orderController.searchOrders(
                "three", "2026-04-01T00:00:00", "2026-04-30T23:59:59", session);

        assertEquals(HttpStatus.OK, success.getStatusCode());
        assertEquals(List.of(order), success.getBody().get("data"));

        ResponseEntity<Map<String, Object>> badDate = orderController.searchOrders(
                "three", "not-a-date", null, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, badDate.getStatusCode());
    }

    @Test
    void adminOrderApisShouldCheckLoginRoleAndDelegate() {
        when(authService.getCurrentUser(session)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, orderController.getAllOrdersForAdmin(session).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                orderController.searchAllOrdersForAdmin("three", null, null, session).getStatusCode());

        when(authService.getCurrentUser(session)).thenReturn(user);
        assertEquals(HttpStatus.FORBIDDEN, orderController.getAllOrdersForAdmin(session).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                orderController.searchAllOrdersForAdmin("three", null, null, session).getStatusCode());

        when(authService.getCurrentUser(session)).thenReturn(admin);
        when(orderService.getAllOrders()).thenReturn(List.of(order));
        ResponseEntity<Map<String, Object>> allOrders = orderController.getAllOrdersForAdmin(session);
        assertEquals(HttpStatus.OK, allOrders.getStatusCode());
        assertEquals(List.of(order), allOrders.getBody().get("data"));

        when(orderService.searchAllOrders(eq("three"), isNull(), isNull())).thenReturn(List.of(order));
        ResponseEntity<Map<String, Object>> searched = orderController.searchAllOrdersForAdmin(
                "three", null, "", session);
        assertEquals(HttpStatus.OK, searched.getStatusCode());
        assertEquals(List.of(order), searched.getBody().get("data"));
    }

    @Test
    void adminOrderApisShouldReturnServerErrorOnServiceOrDateFailure() {
        when(authService.getCurrentUser(session)).thenReturn(admin);
        when(orderService.getAllOrders()).thenThrow(new RuntimeException("all failed"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, orderController.getAllOrdersForAdmin(session).getStatusCode());

        ResponseEntity<Map<String, Object>> badDate = orderController.searchAllOrdersForAdmin(
                "three", "bad-date", null, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, badDate.getStatusCode());

        when(orderService.searchAllOrders(eq("bad"), isNull(), isNull()))
                .thenThrow(new RuntimeException("search all failed"));
        ResponseEntity<Map<String, Object>> failure = orderController.searchAllOrdersForAdmin(
                "bad", null, null, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
    }

    @Test
    void getOrderByIdShouldReturnDetailOrFailure() {
        when(authService.getCurrentUser(session)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, orderController.getOrderById(1L, session).getStatusCode());

        when(authService.getCurrentUser(session)).thenReturn(user);
        when(orderService.getOrderById(1L)).thenReturn(order);
        assertEquals(order, orderController.getOrderById(1L, session).getBody().get("data"));

        when(orderService.getOrderById(404L)).thenThrow(new RuntimeException("missing"));
        ResponseEntity<Map<String, Object>> failure = orderController.getOrderById(404L, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
    }

    @Test
    void createOrderShouldCoverDirectBuyAndCartCheckoutBranches() {
        when(authService.getCurrentUser(session)).thenReturn(null);
        assertEquals(HttpStatus.UNAUTHORIZED, orderController.createOrder(Map.of(), session).getStatusCode());

        when(authService.getCurrentUser(session)).thenReturn(user);

        ResponseEntity<Map<String, Object>> emptyDirect = orderController.createOrder(
                Map.of("directBuy", true, "items", List.of()), session);
        assertEquals(HttpStatus.BAD_REQUEST, emptyDirect.getStatusCode());

        List<Map<String, Object>> directItems = List.of(Map.of("bookId", 100, "quantity", 1));
        when(orderService.createDirectOrder(directItems)).thenReturn(List.of(item));
        ResponseEntity<Map<String, Object>> directSuccess = orderController.createOrder(
                Map.of("directBuy", true, "items", directItems), session);
        assertEquals(HttpStatus.OK, directSuccess.getStatusCode());
        assertEquals(List.of(item), directSuccess.getBody().get("data"));

        ResponseEntity<Map<String, Object>> emptyCart = orderController.createOrder(Map.of(), session);
        assertEquals(HttpStatus.BAD_REQUEST, emptyCart.getStatusCode());

        when(orderService.createOrder(List.of(1L, 2L, 3L))).thenReturn(List.of(item));
        ResponseEntity<Map<String, Object>> cartSuccess = orderController.createOrder(
                Map.of("cartItemIds", List.of(1, 2L, "3")), session);
        assertEquals(HttpStatus.OK, cartSuccess.getStatusCode());
        assertEquals(List.of(item), cartSuccess.getBody().get("data"));

        ResponseEntity<Map<String, Object>> invalidIds = orderController.createOrder(
                Map.of("cartItemIds", List.of("bad-id")), session);
        assertEquals(HttpStatus.BAD_REQUEST, invalidIds.getStatusCode());
    }

    @Test
    void createOrderShouldReturnServerErrorWhenServiceThrows() {
        when(authService.getCurrentUser(session)).thenReturn(user);
        when(orderService.createOrder(List.of(9L))).thenThrow(new RuntimeException("create failed"));

        ResponseEntity<Map<String, Object>> failure = orderController.createOrder(
                Map.of("cartItemIds", List.of(9)), session);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
        assertTrue(failure.getBody().get("message").toString().contains("create failed"));
    }
}
