package com.ebookstore.service.impl;

import com.ebookstore.dto.BookSalesStatisticsDto;
import com.ebookstore.dto.PersonalStatisticsDto;
import com.ebookstore.dto.UserConsumptionStatisticsDto;
import com.ebookstore.entity.Book;
import com.ebookstore.entity.Order;
import com.ebookstore.entity.OrderItem;
import com.ebookstore.entity.User;
import com.ebookstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private User coco;
    private User tom;
    private Book threeBody;
    private Book cleanCode;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.of(2026, 4, 1, 0, 0);
        endDate = LocalDateTime.of(2026, 4, 30, 23, 59);

        coco = buildUser(1L, "Coco", "coco@example.com");
        tom = buildUser(2L, "Tom", "tom@example.com");

        threeBody = buildBook(100L, "Three Body", "Liu Cixin", "/images/threebody.jpg");
        cleanCode = buildBook(101L, "Clean Code", "Robert Martin", "/images/cleancode.jpg");
    }

    @Test
    void getBookSalesStatisticsShouldReturnEmptyListWhenNoOrdersExist() {
        when(orderRepository.findByOrderDateBetween(startDate, endDate)).thenReturn(List.of());

        List<BookSalesStatisticsDto> result = statisticsService.getBookSalesStatistics(startDate, endDate);

        assertTrue(result.isEmpty());
    }

    @Test
    void getBookSalesStatisticsShouldAggregateSortAndCalculateAveragePrice() {
        Order order1 = buildOrder(1L, coco, startDate.plusDays(1),
                buildOrderItem(11L, threeBody, 2, "88.00"),
                buildOrderItem(12L, cleanCode, 1, "66.00"));
        Order order2 = buildOrder(2L, tom, startDate.plusDays(2),
                buildOrderItem(13L, threeBody, 1, "88.00"));

        when(orderRepository.findByOrderDateBetween(startDate, endDate)).thenReturn(List.of(order1, order2));

        List<BookSalesStatisticsDto> result = statisticsService.getBookSalesStatistics(startDate, endDate);

        assertEquals(2, result.size());
        assertEquals("Three Body", result.get(0).getBookTitle());
        assertEquals(3L, result.get(0).getTotalSales());
        assertEquals(new BigDecimal("264.00"), result.get(0).getTotalRevenue());
        assertEquals(new BigDecimal("88.00"), result.get(0).getAveragePrice());

        assertEquals("Clean Code", result.get(1).getBookTitle());
        assertEquals(1L, result.get(1).getTotalSales());
        assertEquals(new BigDecimal("66.00"), result.get(1).getAveragePrice());
    }

    @Test
    void getUserConsumptionStatisticsShouldAggregateUserOrdersAndSortByAmount() {
        Order cocoOrder1 = buildOrder(1L, coco, startDate.plusDays(1),
                buildOrderItem(11L, threeBody, 2, "88.00"));
        Order cocoOrder2 = buildOrder(2L, coco, startDate.plusDays(2),
                buildOrderItem(12L, cleanCode, 1, "66.00"));
        Order tomOrder = buildOrder(3L, tom, startDate.plusDays(3),
                buildOrderItem(13L, cleanCode, 1, "66.00"));

        when(orderRepository.findByOrderDateBetween(startDate, endDate)).thenReturn(List.of(tomOrder, cocoOrder1, cocoOrder2));

        List<UserConsumptionStatisticsDto> result = statisticsService.getUserConsumptionStatistics(startDate, endDate);

        assertEquals(2, result.size());
        assertEquals("Coco", result.get(0).getUserName());
        assertEquals(2L, result.get(0).getTotalOrders());
        assertEquals(3L, result.get(0).getTotalBooks());
        assertEquals(new BigDecimal("242.00"), result.get(0).getTotalConsumption());
        assertEquals(new BigDecimal("121.00"), result.get(0).getAverageOrderValue());

        assertEquals("Tom", result.get(1).getUserName());
        assertEquals(new BigDecimal("66.00"), result.get(1).getTotalConsumption());
    }

    @Test
    void getUserConsumptionStatisticsShouldReturnEmptyListWhenNoOrdersExist() {
        when(orderRepository.findByOrderDateBetween(startDate, endDate)).thenReturn(List.of());

        List<UserConsumptionStatisticsDto> result = statisticsService.getUserConsumptionStatistics(startDate, endDate);

        assertTrue(result.isEmpty());
    }

    @Test
    void getPersonalStatisticsShouldReturnZeroValuesWhenUserHasNoOrders() {
        when(orderRepository.findByUserIdAndOrderDateBetween(1L, startDate, endDate)).thenReturn(List.of());

        PersonalStatisticsDto result = statisticsService.getPersonalStatistics(1L, startDate, endDate);

        assertEquals(0L, result.getTotalOrders());
        assertEquals(0L, result.getTotalBooks());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
        assertTrue(result.getBookDetails().isEmpty());
    }

    @Test
    void getPersonalStatisticsShouldAggregateBookDetailsAndSortByQuantity() {
        Order order1 = buildOrder(1L, coco, startDate.plusDays(1),
                buildOrderItem(11L, threeBody, 2, "88.00"),
                buildOrderItem(12L, cleanCode, 1, "66.00"));
        Order order2 = buildOrder(2L, coco, startDate.plusDays(2),
                buildOrderItem(13L, threeBody, 1, "88.00"));

        when(orderRepository.findByUserIdAndOrderDateBetween(1L, startDate, endDate)).thenReturn(List.of(order1, order2));

        PersonalStatisticsDto result = statisticsService.getPersonalStatistics(1L, startDate, endDate);

        assertEquals(2L, result.getTotalOrders());
        assertEquals(4L, result.getTotalBooks());
        assertEquals(new BigDecimal("330.00"), result.getTotalAmount());
        assertEquals(2, result.getBookDetails().size());

        PersonalStatisticsDto.BookPurchaseDto topBook = result.getBookDetails().get(0);
        assertEquals("Three Body", topBook.getBookTitle());
        assertEquals(3L, topBook.getQuantity());
        assertEquals(new BigDecimal("264.00"), topBook.getTotalAmount());
        assertEquals(new BigDecimal("88.00"), topBook.getAveragePrice());
    }

    private User buildUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Book buildBook(Long id, String title, String author, String cover) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setCover(cover);
        return book;
    }

    private Order buildOrder(Long id, User user, LocalDateTime orderDate, OrderItem... items) {
        Order order = new Order();
        order.setId(id);
        order.setUser(user);
        order.setOrderDate(orderDate);
        order.setStatus("COMPLETED");
        order.setTotalAmount(BigDecimal.ZERO);

        for (OrderItem item : items) {
            order.addOrderItem(item);
            order.setTotalAmount(order.getTotalAmount().add(item.getSubtotal()));
        }

        return order;
    }

    private OrderItem buildOrderItem(Long id, Book book, int quantity, String price) {
        BigDecimal unitPrice = new BigDecimal(price);
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setBook(book);
        item.setQuantity(quantity);
        item.setPrice(unitPrice);
        item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return item;
    }
}
