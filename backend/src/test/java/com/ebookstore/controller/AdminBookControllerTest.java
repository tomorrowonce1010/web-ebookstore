package com.ebookstore.controller;

import com.ebookstore.entity.Book;
import com.ebookstore.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBookControllerTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private AdminBookController adminBookController;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(100L);
        book.setTitle("Three Body");
        book.setAuthor("Liu Cixin");
        book.setPrice(new BigDecimal("88.00"));
        book.setDeleted(false);
        book.setStock(10);
    }

    @Test
    void getAllBooksShouldReturnPageMetadataAndFailureMap() {
        when(bookService.getAllBooksForAdmin(any())).thenReturn(new PageImpl<>(List.of(book)));
        ResponseEntity<Map<String, Object>> success = adminBookController.getAllBooks(0, 10);
        assertEquals(true, success.getBody().get("success"));
        assertEquals(1L, success.getBody().get("total"));

        when(bookService.getAllBooksForAdmin(any())).thenThrow(new RuntimeException("page failed"));
        ResponseEntity<Map<String, Object>> failure = adminBookController.getAllBooks(0, 10);
        assertEquals(false, failure.getBody().get("success"));
    }

    @Test
    void shouldHandleBookDetailAddAndUpdateBranches() {
        when(bookService.getBookEntityById(100L)).thenReturn(book);
        assertEquals(book, adminBookController.getBookById(100L).getBody().get("data"));

        when(bookService.getBookEntityById(404L)).thenThrow(new RuntimeException("missing"));
        assertEquals(false, adminBookController.getBookById(404L).getBody().get("success"));

        when(bookService.saveBook(book)).thenReturn(book);
        assertEquals(book, adminBookController.addBook(book).getBody().get("data"));
        assertEquals(book, adminBookController.updateBook(100L, book).getBody().get("data"));
        assertEquals(100L, book.getId());
    }

    @Test
    void addAndUpdateBookShouldReturnFailureMapWhenServiceThrows() {
        when(bookService.saveBook(book)).thenThrow(new RuntimeException("save failed"));

        assertEquals(false, adminBookController.addBook(book).getBody().get("success"));
        assertEquals(false, adminBookController.updateBook(100L, book).getBody().get("success"));
    }

    @Test
    void toggleBookStatusShouldSoftDeleteRestoreAndHandleServiceFailure() {
        when(bookService.getBookEntityById(100L)).thenReturn(book);
        when(bookService.softDeleteBook(100L)).thenReturn(true);
        ResponseEntity<Map<String, Object>> deleted = adminBookController.toggleBookStatus(100L);
        assertEquals(true, deleted.getBody().get("success"));

        Book deletedBook = new Book();
        deletedBook.setId(101L);
        deletedBook.setDeleted(true);
        when(bookService.getBookEntityById(101L)).thenReturn(deletedBook);
        when(bookService.restoreBook(101L)).thenReturn(false);
        ResponseEntity<Map<String, Object>> restoreFailed = adminBookController.toggleBookStatus(101L);
        assertEquals(false, restoreFailed.getBody().get("success"));

        when(bookService.getBookEntityById(404L)).thenThrow(new RuntimeException("toggle failed"));
        assertEquals(false, adminBookController.toggleBookStatus(404L).getBody().get("success"));
    }

    @Test
    void searchStockUpdateAndStockCheckShouldCoverSuccessAndFailure() {
        when(bookService.searchBooksForAdmin("three")).thenReturn(List.of(book));
        assertEquals(1, adminBookController.searchBooks("three").getBody().get("total"));

        when(bookService.searchBooksForAdmin("bad")).thenThrow(new RuntimeException("search failed"));
        assertEquals(false, adminBookController.searchBooks("bad").getBody().get("success"));

        when(bookService.updateStock(100L, 5)).thenReturn(true);
        assertEquals(true, adminBookController.updateBookStock(100L, 5).getBody().get("success"));

        when(bookService.updateStock(100L, 0)).thenReturn(false);
        assertEquals(false, adminBookController.updateBookStock(100L, 0).getBody().get("success"));

        when(bookService.checkStock(100L, 2)).thenReturn(true);
        assertEquals(true, adminBookController.checkBookStock(100L, 2).getBody().get("available"));

        when(bookService.checkStock(100L, 99)).thenReturn(false);
        assertEquals(false, adminBookController.checkBookStock(100L, 99).getBody().get("available"));
    }

    @Test
    void stockApisShouldReturnFailureMapWhenServiceThrows() {
        when(bookService.updateStock(404L, 1)).thenThrow(new RuntimeException("stock update failed"));
        assertEquals(false, adminBookController.updateBookStock(404L, 1).getBody().get("success"));

        when(bookService.checkStock(404L, 1)).thenThrow(new RuntimeException("stock check failed"));
        assertEquals(false, adminBookController.checkBookStock(404L, 1).getBody().get("success"));
    }
}
