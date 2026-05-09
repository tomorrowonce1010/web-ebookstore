package com.ebookstore.interceptor;

import com.ebookstore.dto.UserInfoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AdminInterceptorTest {

    private final AdminInterceptor interceptor = new AdminInterceptor();

    @Test
    void shouldAllowOptionsAndAdminUser() throws Exception {
        assertTrue(interceptor.preHandle(new MockHttpServletRequest("OPTIONS", "/api/admin/books"), new MockHttpServletResponse(), new Object()));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", new UserInfoDTO(1L, "Admin", "admin@example.com", "Shanghai", "13800000000", "admin", "ADMIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/books");
        request.setSession(session);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void shouldRejectAnonymousAndNormalUser() throws Exception {
        MockHttpServletResponse anonymousResponse = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(new MockHttpServletRequest("GET", "/api/admin/books"), anonymousResponse, new Object()));
        assertEquals(HttpServletResponse.SC_FORBIDDEN, anonymousResponse.getStatus());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", new UserInfoDTO(2L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/books");
        request.setSession(session);
        MockHttpServletResponse userResponse = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, userResponse, new Object()));
        assertEquals(HttpServletResponse.SC_FORBIDDEN, userResponse.getStatus());
    }
}
