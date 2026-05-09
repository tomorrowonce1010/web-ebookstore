package com.ebookstore.dao.impl;

import com.ebookstore.entity.User;
import com.ebookstore.entity.UserAuth;
import com.ebookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDaoImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<User> userQuery;

    @Mock
    private TypedQuery<Long> countQuery;

    private UserDaoImpl userDao;

    private User user;

    @BeforeEach
    void setUp() {
        userDao = new UserDaoImpl();
        ReflectionTestUtils.setField(userDao, "userRepository", userRepository);
        ReflectionTestUtils.setField(userDao, "entityManager", entityManager);

        user = new User();
        user.setId(1L);
        user.setName("Coco");
        user.setEmail("coco@example.com");
    }

    @Test
    void findByIdShouldReturnOptionalFromEntityManager() {
        when(entityManager.find(User.class, 1L)).thenReturn(user);
        when(entityManager.find(User.class, 404L)).thenReturn(null);

        assertEquals(Optional.of(user), userDao.findById(1L));
        assertTrue(userDao.findById(404L).isEmpty());
    }

    @Test
    void findByEmailShouldReturnUserOrEmptyWhenQueryThrows() {
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(userQuery);
        when(userQuery.setParameter("email", "coco@example.com")).thenReturn(userQuery);
        when(userQuery.getSingleResult()).thenReturn(user);

        assertEquals(Optional.of(user), userDao.findByEmail("coco@example.com"));

        when(userQuery.setParameter("email", "missing@example.com")).thenReturn(userQuery);
        when(userQuery.getSingleResult()).thenThrow(new NoResultException("missing"));

        assertTrue(userDao.findByEmail("missing@example.com").isEmpty());
    }

    @Test
    void shouldDelegateFindByNameDeleteAndFindAllToRepository() {
        when(userRepository.findByName("Coco")).thenReturn(Optional.of(user));
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertEquals(Optional.of(user), userDao.findByName("Coco"));
        assertEquals(List.of(user), userDao.findAll());

        userDao.deleteById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void saveShouldPersistNewUserAndMergeExistingUser() {
        User newUser = new User();
        newUser.setName("New");

        assertSame(newUser, userDao.save(newUser));
        verify(entityManager).persist(newUser);

        User merged = new User();
        merged.setId(1L);
        merged.setName("Merged");
        when(entityManager.merge(user)).thenReturn(merged);

        assertSame(merged, userDao.save(user));
    }

    @Test
    void existsByEmailShouldUseCountQuery() {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.setParameter("email", "coco@example.com")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);

        assertTrue(userDao.existsByEmail("coco@example.com"));

        when(countQuery.setParameter("email", "missing@example.com")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        assertFalse(userDao.existsByEmail("missing@example.com"));
    }

    @Test
    void findByUserAuthShouldReturnUserOrEmptyWhenQueryThrows() {
        UserAuth auth = new UserAuth();
        auth.setId(10L);

        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(userQuery);
        when(userQuery.setParameter("authId", 10L)).thenReturn(userQuery);
        when(userQuery.getSingleResult()).thenReturn(user);

        assertEquals(Optional.of(user), userDao.findByUserAuth(auth));

        when(userQuery.getSingleResult()).thenThrow(new NoResultException("missing"));

        assertTrue(userDao.findByUserAuth(auth).isEmpty());
    }
}
