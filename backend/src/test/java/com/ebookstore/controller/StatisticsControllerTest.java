package com.ebookstore.controller;

import com.ebookstore.dto.BookSalesStatisticsDto;
import com.ebookstore.dto.PersonalStatisticsDto;
import com.ebookstore.dto.UserConsumptionStatisticsDto;
import com.ebookstore.dto.UserInfoDTO;
import com.ebookstore.service.StatisticsService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private StatisticsController statisticsController;

    private MockHttpSession session;
    private UserInfoDTO user;
    private UserInfoDTO admin;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        user = new UserInfoDTO(1L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER");
        admin = new UserInfoDTO(2L, "Admin", "admin@example.com", "Shanghai", "13900000000", "admin", "ADMIN");
        start = LocalDateTime.of(2026, 4, 1, 0, 0);
        end = LocalDateTime.of(2026, 4, 30, 23, 59);
    }

    @Test
    void bookSalesStatisticsShouldCheckAdminPermissionAndHandleFailure() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                statisticsController.getBookSalesStatistics(start, end, session).getStatusCode());

        session.setAttribute("currentUser", user);
        assertEquals(HttpStatus.FORBIDDEN,
                statisticsController.getBookSalesStatistics(start, end, session).getStatusCode());

        BookSalesStatisticsDto dto = new BookSalesStatisticsDto(
                100L, "Three Body", "Liu Cixin", "cover.jpg", 3L,
                new BigDecimal("264.00"), new BigDecimal("88.00"));
        session.setAttribute("currentUser", admin);
        when(statisticsService.getBookSalesStatistics(start, end)).thenReturn(List.of(dto));
        ResponseEntity<?> success = statisticsController.getBookSalesStatistics(start, end, session);
        assertEquals(HttpStatus.OK, success.getStatusCode());
        assertEquals(List.of(dto), success.getBody());

        when(statisticsService.getBookSalesStatistics(start, end)).thenThrow(new RuntimeException("stats failed"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                statisticsController.getBookSalesStatistics(start, end, session).getStatusCode());
    }

    @Test
    void userConsumptionStatisticsShouldCheckAdminPermissionAndReturnData() {
        session.setAttribute("currentUser", user);
        assertEquals(HttpStatus.FORBIDDEN,
                statisticsController.getUserConsumptionStatistics(start, end, session).getStatusCode());

        UserConsumptionStatisticsDto dto = new UserConsumptionStatisticsDto(
                1L, "Coco", "coco@example.com", 2L, 3L,
                new BigDecimal("176.00"), new BigDecimal("88.00"));
        session.setAttribute("currentUser", admin);
        when(statisticsService.getUserConsumptionStatistics(start, end)).thenReturn(List.of(dto));

        ResponseEntity<?> success = statisticsController.getUserConsumptionStatistics(start, end, session);

        assertEquals(HttpStatus.OK, success.getStatusCode());
        assertEquals(List.of(dto), success.getBody());
    }

    @Test
    void personalStatisticsShouldRequireLoginAndHandleSuccessAndFailure() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                statisticsController.getPersonalStatistics(start, end, session).getStatusCode());

        PersonalStatisticsDto dto = new PersonalStatisticsDto(
                3L, new BigDecimal("264.00"), 2L, List.of());
        session.setAttribute("currentUser", user);
        when(statisticsService.getPersonalStatistics(1L, start, end)).thenReturn(dto);
        ResponseEntity<?> success = statisticsController.getPersonalStatistics(start, end, session);
        assertEquals(HttpStatus.OK, success.getStatusCode());
        assertEquals(dto, success.getBody());

        when(statisticsService.getPersonalStatistics(1L, start, end)).thenThrow(new RuntimeException("personal failed"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                statisticsController.getPersonalStatistics(start, end, session).getStatusCode());
    }
}
