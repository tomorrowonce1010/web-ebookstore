package com.ebookstore.service.impl;

import com.ebookstore.dto.OrderDTO;
import com.ebookstore.dto.OrderItemDTO;
import com.ebookstore.entity.Book;
import com.ebookstore.entity.CartItem;
import com.ebookstore.entity.Order;
import com.ebookstore.entity.User;
import com.ebookstore.entity.UserAuth;
import com.ebookstore.repository.BookRepository;
import com.ebookstore.repository.CartItemRepository;
import com.ebookstore.repository.OrderRepository;
import com.ebookstore.service.BookService;
import com.ebookstore.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserService userService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookService bookService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        UserAuth userAuth = new UserAuth();
        userAuth.setId(10L);
        userAuth.setUsername("coco");
        userAuth.setRole("USER");
        userAuth.setActive(true);

        user = new User();
        user.setId(1L);
        user.setName("Coco");
        user.setEmail("coco@example.com");
        user.setAddress("Shanghai");
        user.setPhone("13800000000");
        user.setUserAuth(userAuth);

        book = new Book();
        book.setId(100L);
        book.setTitle("Three Body");
        book.setAuthor("Liu Cixin");
        book.setPrice(new BigDecimal("88.00"));
        book.setStock(8);
        book.setStatus("AVAILABLE");
    }

    @Test
    void getOrderByIdShouldThrowWhenOrderBelongsToAnotherUser() {
        User anotherUser = new User();
        anotherUser.setId(2L);

        Order order = new Order();
        order.setId(9L);
        order.setUser(anotherUser);

        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        assertThrows(SecurityException.class, () -> orderService.getOrderById(9L));
    }

    @Test
    void getOrdersShouldFlattenCurrentUserOrderItems() {
        Order order = buildOrder(301L, user, book, 2, LocalDateTime.of(2026, 4, 23, 10, 0));
        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findByUserOrderByOrderDateDesc(user)).thenReturn(List.of(order));

        List<OrderItemDTO> result = orderService.getOrders();

        assertEquals(1, result.size());
        assertEquals("Three Body", result.get(0).getTitle());
        assertEquals(2, result.get(0).getQuantity());
    }

    @Test
    void getOrderByIdShouldReturnDtoWhenOrderBelongsToCurrentUser() {
        Order order = buildOrder(302L, user, book, 1, LocalDateTime.of(2026, 4, 23, 11, 0));
        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findById(302L)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.getOrderById(302L);

        assertEquals(302L, result.getId());
        assertEquals("coco", result.getUser().getUsername());
        assertEquals(1, result.getOrderItems().size());
    }

    @Test
    void getOrderByIdShouldThrowWhenOrderDoesNotExist() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.getOrderById(404L));
    }

    @Test
    void createOrderShouldThrowWhenNoSelectedItemsExist() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findByUserAndSelected(user, true)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(null));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrderShouldThrowWhenStockIsInsufficient() {
        CartItem selectedItem = buildCartItem(11L, user, book, 2, true);

        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findByUserAndSelected(user, true)).thenReturn(List.of(selectedItem));
        when(bookService.checkStock(100L, 2)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(null));

        assertNotNull(exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrderShouldSaveOrderReduceStockAndClearCart() {
        CartItem selectedItem = buildCartItem(11L, user, book, 2, true);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findByUserAndSelected(user, true)).thenReturn(List.of(selectedItem));
        when(bookService.checkStock(100L, 2)).thenReturn(true);
        when(bookService.reduceStock(100L, 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(200L);
            return order;
        });

        List<OrderItemDTO> result = orderService.createOrder(null);

        assertEquals(1, result.size());
        assertEquals("Three Body", result.get(0).getTitle());
        assertEquals(new BigDecimal("176.00"), result.get(0).getSubtotal());

        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(user, savedOrder.getUser());
        assertEquals("COMPLETED", savedOrder.getStatus());
        assertEquals(new BigDecimal("176.00"), savedOrder.getTotalAmount());
        assertEquals("Shanghai", savedOrder.getShippingAddress());
        assertEquals(1, savedOrder.getItems().size());

        verify(bookService).reduceStock(100L, 2);
        verify(cartItemRepository).deleteAll(List.of(selectedItem));
    }

    @Test
    void createOrderShouldUseOnlyCurrentUserItemsWhenIdsAreProvided() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        CartItem ownedItem = buildCartItem(11L, user, book, 1, false);
        CartItem anotherUserItem = buildCartItem(12L, anotherUser, book, 1, false);

        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findAllById(List.of(11L, 12L))).thenReturn(List.of(ownedItem, anotherUserItem));
        when(bookService.checkStock(100L, 1)).thenReturn(true);
        when(bookService.reduceStock(100L, 1)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(202L);
            return order;
        });

        List<OrderItemDTO> result = orderService.createOrder(List.of(11L, 12L));

        assertEquals(1, result.size());
        verify(cartItemRepository).deleteAll(List.of(ownedItem));
    }

    @Test
    void createOrderShouldWrapExceptionWhenReducingStockFails() {
        CartItem selectedItem = buildCartItem(11L, user, book, 2, true);

        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findByUserAndSelected(user, true)).thenReturn(List.of(selectedItem));
        when(bookService.checkStock(100L, 2)).thenReturn(true);
        when(bookService.reduceStock(100L, 2)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.createOrder(null));

        assertNotNull(exception.getCause());
        verify(cartItemRepository, never()).deleteAll(anyList());
    }

    @Test
    void createDirectOrderShouldCreateOrderWhenInventoryIsEnough() {
        Map<String, Object> item = new HashMap<>();
        item.put("bookId", 100L);
        item.put("quantity", 3);

        when(userService.getCurrentUser()).thenReturn(user);
        when(bookService.checkStock(100L, 3)).thenReturn(true);
        when(bookService.reduceStock(100L, 3)).thenReturn(true);
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(201L);
            return order;
        });

        List<OrderItemDTO> result = orderService.createDirectOrder(List.of(item));

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("264.00"), result.get(0).getSubtotal());
        verify(bookService).reduceStock(100L, 3);
    }

    @Test
    void createDirectOrderShouldThrowWhenInventoryIsInsufficient() {
        Map<String, Object> item = new HashMap<>();
        item.put("bookId", 100L);
        item.put("quantity", 9);

        when(userService.getCurrentUser()).thenReturn(user);
        when(bookService.checkStock(100L, 9)).thenReturn(false);
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));

        assertThrows(IllegalArgumentException.class, () -> orderService.createDirectOrder(List.of(item)));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createDirectOrderShouldWrapExceptionWhenReducingStockFails() {
        Map<String, Object> item = new HashMap<>();
        item.put("bookId", 100L);
        item.put("quantity", 1);

        when(userService.getCurrentUser()).thenReturn(user);
        when(bookService.checkStock(100L, 1)).thenReturn(true);
        when(bookService.reduceStock(100L, 1)).thenReturn(false);
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.createDirectOrder(List.of(item)));

        assertNotNull(exception.getCause());
    }

    @Test
    void createDirectOrderShouldWrapExceptionWhenOrderSaveFails() {
        Map<String, Object> item = new HashMap<>();
        item.put("bookId", 100L);
        item.put("quantity", 1);

        when(userService.getCurrentUser()).thenReturn(user);
        when(bookService.checkStock(100L, 1)).thenReturn(true);
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("save failed"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.createDirectOrder(List.of(item)));

        assertTrue(exception.getMessage().contains("创建直接订单失败"));
    }

    @Test
    void searchUserOrdersShouldConvertOrdersToDtos() {
        Order order = buildOrder(301L, user, book, 1, LocalDateTime.of(2026, 4, 23, 10, 0));
        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findByUserAndBookNameAndDateRange(user, "Three", null, null)).thenReturn(List.of(order));

        List<OrderDTO> result = orderService.searchUserOrders("Three", null, null);

        assertEquals(1, result.size());
        assertEquals(301L, result.get(0).getId());
        assertEquals("coco", result.get(0).getUser().getUsername());
        assertEquals(1, result.get(0).getOrderItems().size());
    }

    @Test
    void getAllOrdersShouldConvertRepositoryResults() {
        Order order = buildOrder(401L, user, book, 1, LocalDateTime.of(2026, 4, 23, 12, 0));
        when(orderRepository.findAllByOrderByOrderDateDesc()).thenReturn(List.of(order));

        List<OrderDTO> result = orderService.getAllOrders();

        assertEquals(1, result.size());
        assertEquals(401L, result.get(0).getId());
        assertEquals("coco", result.get(0).getUser().getUsername());
    }

    @Test
    void searchAllOrdersShouldConvertRepositoryResults() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 30, 23, 59);
        Order order = buildOrder(402L, user, book, 1, LocalDateTime.of(2026, 4, 23, 12, 0));

        when(orderRepository.findByBookNameAndDateRange("Three", start, end)).thenReturn(List.of(order));

        List<OrderDTO> result = orderService.searchAllOrders("Three", start, end);

        assertEquals(1, result.size());
        assertEquals(402L, result.get(0).getId());
        assertEquals(1, result.get(0).getOrderItems().size());
    }

    private CartItem buildCartItem(Long id, User owner, Book targetBook, int quantity, boolean selected) {
        CartItem cartItem = new CartItem();
        cartItem.setId(id);
        cartItem.setUser(owner);
        cartItem.setBook(targetBook);
        cartItem.setQuantity(quantity);
        cartItem.setSelected(selected);
        return cartItem;
    }

    private Order buildOrder(Long id, User owner, Book targetBook, int quantity, LocalDateTime orderDate) {
        Order order = new Order();
        order.setId(id);
        order.setUser(owner);
        order.setOrderDate(orderDate);
        order.setStatus("COMPLETED");
        order.setShippingAddress(owner.getAddress());
        order.setTotalAmount(targetBook.getPrice().multiply(BigDecimal.valueOf(quantity)));

        com.ebookstore.entity.OrderItem orderItem = new com.ebookstore.entity.OrderItem();
        orderItem.setId(id + 1);
        orderItem.setBook(targetBook);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(targetBook.getPrice());
        orderItem.setSubtotal(targetBook.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.addOrderItem(orderItem);

        return order;
    }
}
