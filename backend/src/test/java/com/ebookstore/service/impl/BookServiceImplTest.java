package com.ebookstore.service.impl;

import com.ebookstore.dto.BookDTO;
import com.ebookstore.entity.Book;
import com.ebookstore.repository.BookRepository;
import com.ebookstore.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(100L);
        book.setTitle("Three Body");
        book.setAuthor("Liu Cixin");
        book.setPrice(new BigDecimal("88.00"));
        book.setDescription("Sci-fi");
        book.setCover("/images/threebody.jpg");
        book.setStatus("AVAILABLE");
        book.setStock(10);
        book.setIsbn("9787536692930");
        book.setDeleted(false);
    }

    @Test
    void getAllBooksShouldConvertEntitiesToDtos() {
        when(bookRepository.findAllAvailable()).thenReturn(List.of(book));

        List<BookDTO> result = bookService.getAllBooks();

        assertEquals(1, result.size());
        assertEquals("Three Body", result.get(0).getTitle());
        assertEquals("Liu Cixin", result.get(0).getAuthor());
    }

    @Test
    void getBookByIdShouldReturnDtoWhenBookExists() {
        when(bookRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(book));

        BookDTO result = bookService.getBookById(100L);

        assertEquals(100L, result.getId());
        assertEquals("Three Body", result.getTitle());
    }

    @Test
    void getBookByIdShouldThrowWhenBookDoesNotExist() {
        when(bookRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bookService.getBookById(100L));
    }

    @Test
    void searchBooksShouldDelegateAndConvertResults() {
        when(bookRepository.searchAvailableBooks("three")).thenReturn(List.of(book));

        List<BookDTO> result = bookService.searchBooks("three");

        assertEquals(1, result.size());
        assertEquals("Three Body", result.get(0).getTitle());
    }

    @Test
    void saveBookShouldDelegateToRepository() {
        when(bookRepository.save(book)).thenReturn(book);

        Book result = bookService.saveBook(book);

        assertSame(book, result);
    }

    @Test
    void softDeleteBookShouldMarkBookDeletedAndCleanCart() {
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartService.cleanCartByBookId(100L)).thenReturn(2);

        boolean result = bookService.softDeleteBook(100L);

        assertTrue(result);
        verify(bookRepository).save(bookCaptor.capture());
        assertTrue(bookCaptor.getValue().getDeleted());
        verify(cartService).cleanCartByBookId(100L);
    }

    @Test
    void softDeleteBookShouldReturnFalseWhenBookDoesNotExist() {
        when(bookRepository.findById(100L)).thenReturn(Optional.empty());

        boolean result = bookService.softDeleteBook(100L);

        assertFalse(result);
        verify(cartService, never()).cleanCartByBookId(anyLong());
    }

    @Test
    void restoreBookShouldMarkBookAsNotDeleted() {
        book.setDeleted(true);
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = bookService.restoreBook(100L);

        assertTrue(result);
        assertFalse(book.getDeleted());
    }

    @Test
    void restoreBookShouldReturnFalseWhenRepositoryThrows() {
        when(bookRepository.findById(100L)).thenThrow(new RuntimeException("db error"));

        assertFalse(bookService.restoreBook(100L));
    }

    @Test
    void getAllBooksForAdminShouldReturnRepositoryPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Book> page = new PageImpl<>(List.of(book), pageable, 1);
        when(bookRepository.findAllForAdmin(pageable)).thenReturn(page);

        Page<Book> result = bookService.getAllBooksForAdmin(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getBookEntityByIdShouldReturnDeletedOrAvailableBookForAdmin() {
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));

        Book result = bookService.getBookEntityById(100L);

        assertSame(book, result);
    }

    @Test
    void getBookEntityByIdShouldThrowWhenMissing() {
        when(bookRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bookService.getBookEntityById(100L));
    }

    @Test
    void searchBooksForAdminShouldIncludeRepositoryResults() {
        when(bookRepository.searchBooks("three")).thenReturn(List.of(book));

        List<Book> result = bookService.searchBooksForAdmin("three");

        assertEquals(1, result.size());
        assertSame(book, result.get(0));
    }

    @Test
    void updateStockShouldSwitchStatusBasedOnQuantity() {
        when(bookRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean zeroResult = bookService.updateStock(100L, 0);
        assertTrue(zeroResult);
        assertEquals("OUT_OF_STOCK", book.getStatus());
        assertEquals(0, book.getStock());

        boolean positiveResult = bookService.updateStock(100L, 5);
        assertTrue(positiveResult);
        assertEquals("AVAILABLE", book.getStatus());
        assertEquals(5, book.getStock());
    }

    @Test
    void reduceStockShouldReturnFalseWhenInventoryIsInsufficient() {
        when(bookRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(book));

        boolean result = bookService.reduceStock(100L, 11);

        assertFalse(result);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void reduceStockShouldSetOutOfStockWhenInventoryDropsToZero() {
        book.setStock(2);
        when(bookRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = bookService.reduceStock(100L, 2);

        assertTrue(result);
        assertEquals(0, book.getStock());
        assertEquals("OUT_OF_STOCK", book.getStatus());
    }

    @Test
    void checkStockShouldReturnFalseWhenBookCannotBeLoaded() {
        when(bookRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.empty());

        assertFalse(bookService.checkStock(100L, 1));
    }

    @Test
    void checkStockShouldReturnTrueWhenInventoryIsEnough() {
        when(bookRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(book));

        assertTrue(bookService.checkStock(100L, 10));
    }

    @Test
    void updateStockShouldReturnFalseWhenBookCannotBeLoaded() {
        when(bookRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.empty());

        assertFalse(bookService.updateStock(100L, 1));
    }

    @Test
    void reduceStockShouldReturnFalseWhenBookCannotBeLoaded() {
        when(bookRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.empty());

        assertFalse(bookService.reduceStock(100L, 1));
    }

    @Test
    void getBooksByIdsShouldDelegateToRepository() {
        List<Long> ids = List.of(100L);
        when(bookRepository.findByIdIn(ids)).thenReturn(List.of(book));

        List<Book> result = bookService.getBooksByIds(ids);

        assertEquals(1, result.size());
        assertSame(book, result.get(0));
    }
}
