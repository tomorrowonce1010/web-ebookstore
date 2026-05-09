package com.ebookstore.controller;

import com.ebookstore.dto.BookDTO;
import com.ebookstore.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookController bookController;

    private BookDTO bookDTO;

    @BeforeEach
    void setUp() {
        bookDTO = new BookDTO();
        bookDTO.setId(100L);
        bookDTO.setTitle("Three Body");
        bookDTO.setAuthor("Liu Cixin");
        bookDTO.setPrice(new BigDecimal("88.00"));
    }

    @Test
    void getAllBooksShouldReturnSuccessMapWhenServiceSucceeds() {
        when(bookService.getAllBooks()).thenReturn(List.of(bookDTO));

        ResponseEntity<Map<String, Object>> response = bookController.getAllBooks();

        assertEquals(true, response.getBody().get("success"));
        assertEquals(List.of(bookDTO), response.getBody().get("data"));
    }

    @Test
    void getAllBooksShouldReturnFailureMapWhenServiceThrows() {
        when(bookService.getAllBooks()).thenThrow(new RuntimeException("db error"));

        ResponseEntity<Map<String, Object>> response = bookController.getAllBooks();

        assertEquals(false, response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("db error"));
    }

    @Test
    void getBookByIdShouldReturnSuccessAndFailureMap() {
        when(bookService.getBookById(100L)).thenReturn(bookDTO);
        assertEquals(true, bookController.getBookById(100L).getBody().get("success"));

        when(bookService.getBookById(404L)).thenThrow(new RuntimeException("missing"));
        ResponseEntity<Map<String, Object>> failure = bookController.getBookById(404L);
        assertEquals(false, failure.getBody().get("success"));
    }

    @Test
    void searchBooksShouldReturnSuccessAndFailureMap() {
        when(bookService.searchBooks("three")).thenReturn(List.of(bookDTO));
        assertEquals(true, bookController.searchBooks("three").getBody().get("success"));

        when(bookService.searchBooks("bad")).thenThrow(new RuntimeException("search failed"));
        ResponseEntity<Map<String, Object>> failure = bookController.searchBooks("bad");
        assertEquals(false, failure.getBody().get("success"));
    }
}
