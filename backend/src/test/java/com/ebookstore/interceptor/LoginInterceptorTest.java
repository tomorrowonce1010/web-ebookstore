package com.ebookstore.interceptor;

import com.ebookstore.dto.UserInfoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class LoginInterceptorTest {

    private final LoginInterceptor interceptor = new LoginInterceptor();

    @Test
    void shouldAllowPublicPathsAndOptionsRequests() throws Exception {
        assertTrue(interceptor.preHandle(request("GET", "/api/auth/status", null), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("GET", "/api/books", null), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("OPTIONS", "/api/cart", null), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void shouldAllowProtectedPathWhenSessionHasCurrentUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", new UserInfoDTO(1L, "Coco", "coco@example.com", "Shanghai", "13800000000", "coco", "USER"));

        boolean result = interceptor.preHandle(request("GET", "/api/cart", session), new MockHttpServletResponse(), new Object());

        assertTrue(result);
    }

    @Test
    void shouldRejectProtectedPathWhenUserIsNotLoggedIn() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("GET", "/api/cart", null), response, new Object());

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("success"));
    }

    private MockHttpServletRequest request(String method, String uri, MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (session != null) {
            request.setSession(session);
        }
        return request;
    }
}
