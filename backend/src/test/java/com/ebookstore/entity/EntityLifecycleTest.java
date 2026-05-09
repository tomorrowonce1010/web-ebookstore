package com.ebookstore.entity;

import com.ebookstore.dto.CartItemDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EntityLifecycleTest {

    @Test
    void lifecycleCallbacksShouldFillTimestampsAndDefaults() {
        Book book = new Book();
        book.setDeleted(null);
        book.onCreate();
        assertNotNull(book.getCreatedAt());
        assertNotNull(book.getUpdatedAt());
        assertFalse(book.getDeleted());

        book.onUpdate();
        assertNotNull(book.getUpdatedAt());

        User user = new User();
        user.onCreate();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        user.onUpdate();
        assertNotNull(user.getUpdatedAt());

        UserAuth userAuth = new UserAuth();
        userAuth.onCreate();
        assertNotNull(userAuth.getCreatedAt());

        CartItem cartItem = new CartItem();
        cartItem.onCreate();
        assertNotNull(cartItem.getCreatedAt());
    }

    @Test
    void orderShouldMaintainBidirectionalOrderItemRelationship() {
        Order order = new Order();
        OrderItem item = new OrderItem();

        order.addOrderItem(item);
        assertEquals(1, order.getItems().size());
        assertSame(order, item.getOrder());

        order.removeOrderItem(item);
        assertTrue(order.getItems().isEmpty());
        assertNull(item.getOrder());

        order.onCreate();
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
        assertNotNull(order.getOrderDate());

        order.onUpdate();
        assertNotNull(order.getUpdatedAt());
    }

    @Test
    void cartItemDtoCustomConstructorsShouldPopulateNestedBookInfo() {
        CartItemDTO legacy = new CartItemDTO(
                1L, 100L, "Three Body", "Liu Cixin", new BigDecimal("88.00"),
                "cover.jpg", 2, true);
        assertEquals(100L, legacy.getBook().getId());
        assertEquals("Three Body", legacy.getBook().getTitle());
        assertEquals(2, legacy.getQuantity());
        assertTrue(legacy.getSelected());

        CartItemDTO full = new CartItemDTO(
                2L, 101L, "Dune", "Frank Herbert", new BigDecimal("99.00"),
                "dune.jpg", "AVAILABLE", 8, "ISBN-001", 1, false);
        assertEquals("AVAILABLE", full.getBook().getStatus());
        assertEquals(8, full.getBook().getStock());
        assertEquals("ISBN-001", full.getBook().getIsbn());
        assertFalse(full.getSelected());
    }
}
