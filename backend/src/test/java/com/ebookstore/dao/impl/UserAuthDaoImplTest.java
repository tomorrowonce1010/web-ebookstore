package com.ebookstore.dao.impl;

import com.ebookstore.entity.UserAuth;
import com.ebookstore.repository.UserAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthDaoImplTest {

    @Mock
    private UserAuthRepository userAuthRepository;

    private UserAuthDaoImpl userAuthDao;

    private UserAuth auth;

    @BeforeEach
    void setUp() {
        userAuthDao = new UserAuthDaoImpl();
        ReflectionTestUtils.setField(userAuthDao, "userAuthRepository", userAuthRepository);

        auth = new UserAuth();
        auth.setId(1L);
        auth.setUsername("coco");
    }

    @Test
    void shouldDelegateFindByUsernameSaveFindByIdAndExistsByUsername() {
        when(userAuthRepository.findByUsername("coco")).thenReturn(Optional.of(auth));
        when(userAuthRepository.save(auth)).thenReturn(auth);
        when(userAuthRepository.findById(1L)).thenReturn(Optional.of(auth));
        when(userAuthRepository.existsByUsername("coco")).thenReturn(true);

        assertEquals(Optional.of(auth), userAuthDao.findByUsername("coco"));
        assertSame(auth, userAuthDao.save(auth));
        assertEquals(Optional.of(auth), userAuthDao.findById(1L));
        assertTrue(userAuthDao.existsByUsername("coco"));
    }

    @Test
    void shouldReturnRepositoryEmptyResult() {
        when(userAuthRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertTrue(userAuthDao.findByUsername("missing").isEmpty());
        verify(userAuthRepository).findByUsername("missing");
    }
}
