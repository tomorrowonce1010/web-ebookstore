package com.ebookstore.service.impl;

import com.ebookstore.dto.CartItemDTO;
import com.ebookstore.entity.Book;
import com.ebookstore.entity.CartItem;
import com.ebookstore.entity.User;
import com.ebookstore.repository.BookRepository;
import com.ebookstore.repository.CartItemRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Coco");

        book = new Book();
        book.setId(100L);
        book.setTitle("Three Body");
        book.setAuthor("Liu Cixin");
        book.setPrice(new BigDecimal("88.00"));
        book.setCover("/images/threebody.jpg");
        book.setStatus("AVAILABLE");
        book.setStock(10);
        book.setIsbn("9787536692930");
    }

    @Test
    void getCartItemsShouldThrowWhenUserIsNotLoggedIn() {
        when(userService.getCurrentUser()).thenReturn(null);

        SecurityException exception = assertThrows(SecurityException.class, () -> cartService.getCartItems());

        assertNotNull(exception.getMessage());
        verify(cartItemRepository, never()).findByUser(any(User.class));
    }

    @Test
    void getCartItemsShouldReturnCurrentUserItems() {
        CartItem item = buildCartItem(11L, user, book, 2, true);
        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findByUser(user)).thenReturn(List.of(item));
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));

        List<CartItemDTO> result = cartService.getCartItems();

        assertEquals(1, result.size());
        assertEquals("Three Body", result.get(0).getBook().getTitle());
        assertEquals(2, result.get(0).getQuantity());
        assertTrue(result.get(0).getSelected());
    }

    @Test
    void addToCartShouldThrowWhenUserIsNotLoggedIn() {
        when(userService.getCurrentUser()).thenReturn(null);

        assertThrows(SecurityException.class, () -> cartService.addToCart(100L, 1));
    }

    @Test
    void addToCartShouldIncreaseQuantityWhenItemAlreadyExists() {
        CartItem existingItem = buildCartItem(11L, user, book, 2, false);
        when(userService.getCurrentUser()).thenReturn(user);
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByUserAndBookId(user, 100L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemDTO result = cartService.addToCart(100L, 3);

        assertEquals(5, result.getQuantity());
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void addToCartShouldCreateNewItemWhenItemDoesNotExist() {
        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        when(userService.getCurrentUser()).thenReturn(user);
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByUserAndBookId(user, 100L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });

        CartItemDTO result = cartService.addToCart(100L, 2);

        assertEquals(12L, result.getId());
        assertEquals(2, result.getQuantity());
        assertFalse(result.getSelected());

        verify(cartItemRepository).save(cartItemCaptor.capture());
        CartItem savedItem = cartItemCaptor.getValue();
        assertEquals(user, savedItem.getUser());
        assertEquals(book, savedItem.getBook());
        assertEquals(2, savedItem.getQuantity());
        assertFalse(savedItem.getSelected());
    }

    @Test
    void addToCartShouldThrowWhenBookDoesNotExist() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(bookRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cartService.addToCart(100L, 1));

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateCartItemQuantityShouldDeleteItemWhenQuantityIsNotPositive() {
        CartItem cartItem = buildCartItem(20L, user, book, 3, false);
        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findById(20L)).thenReturn(Optional.of(cartItem));

        cartService.updateCartItemQuantity(20L, 0, 1L);

        verify(cartItemRepository).delete(cartItem);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateCartItemQuantityShouldSavePositiveQuantity() {
        CartItem cartItem = buildCartItem(20L, user, book, 3, false);
        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findById(20L)).thenReturn(Optional.of(cartItem));

        cartService.updateCartItemQuantity(20L, 5, 1L);

        assertEquals(5, cartItem.getQuantity());
        verify(cartItemRepository).save(cartItem);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void updateCartItemQuantityShouldThrowWhenSessionUserDoesNotMatchParameterUser() {
        when(userService.getCurrentUser()).thenReturn(user);

        assertThrows(SecurityException.class, () -> cartService.updateCartItemQuantity(20L, 5, 2L));

        verify(cartItemRepository, never()).findById(anyLong());
    }

    @Test
    void updateCartItemQuantityShouldThrowWhenCartItemBelongsToAnotherUser() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        CartItem cartItem = buildCartItem(20L, anotherUser, book, 3, false);

        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findById(20L)).thenReturn(Optional.of(cartItem));

        assertThrows(SecurityException.class, () -> cartService.updateCartItemQuantity(20L, 5, 1L));

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void removeFromCartShouldThrowWhenUserDoesNotOwnItem() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        CartItem cartItem = buildCartItem(20L, anotherUser, book, 1, false);

        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findById(20L)).thenReturn(Optional.of(cartItem));

        assertThrows(SecurityException.class, () -> cartService.removeFromCart(20L, 1L));

        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void removeFromCartShouldThrowWhenSessionUserDoesNotMatchParameterUser() {
        when(userService.getCurrentUser()).thenReturn(user);

        assertThrows(SecurityException.class, () -> cartService.removeFromCart(20L, 2L));

        verify(cartItemRepository, never()).findById(anyLong());
    }

    @Test
    void removeFromCartShouldDeleteOwnedItem() {
        CartItem cartItem = buildCartItem(20L, user, book, 1, false);
        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findById(20L)).thenReturn(Optional.of(cartItem));

        cartService.removeFromCart(20L, 1L);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void toggleCartItemSelectionShouldThrowWhenUserIsNotLoggedIn() {
        when(userService.getCurrentUser()).thenReturn(null);

        assertThrows(SecurityException.class, () -> cartService.toggleCartItemSelection(21L));

        verify(cartItemRepository, never()).findById(anyLong());
    }

    @Test
    void toggleCartItemSelectionShouldThrowWhenItemBelongsToAnotherUser() {
        User anotherUser = new User();
        anotherUser.setId(99L);
        CartItem cartItem = buildCartItem(21L, anotherUser, book, 1, false);

        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findById(21L)).thenReturn(Optional.of(cartItem));

        assertThrows(SecurityException.class, () -> cartService.toggleCartItemSelection(21L));

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void toggleCartItemSelectionShouldFlipSelectionForOwnedItem() {
        CartItem cartItem = buildCartItem(21L, user, book, 1, false);
        when(userService.getCurrentUser()).thenReturn(user);
        when(cartItemRepository.findById(21L)).thenReturn(Optional.of(cartItem));

        cartService.toggleCartItemSelection(21L);

        assertTrue(cartItem.getSelected());
        verify(cartItemRepository).save(cartItem);
    }

    @Test
    void cleanCartByBookIdShouldDeleteMatchingItemsAndReturnCount() {
        when(cartItemRepository.findByBookId(100L)).thenReturn(
                List.of(
                        buildCartItem(1L, user, book, 1, false),
                        buildCartItem(2L, user, book, 2, true)
                )
        );

        int deletedCount = cartService.cleanCartByBookId(100L);

        assertEquals(2, deletedCount);
        verify(cartItemRepository).deleteByBookId(100L);
    }

    @Test
    void cleanCartByBookIdShouldRejectNullBookId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> cartService.cleanCartByBookId(null));

        assertNotNull(exception.getMessage());
        verify(cartItemRepository, never()).findByBookId(any());
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
}
