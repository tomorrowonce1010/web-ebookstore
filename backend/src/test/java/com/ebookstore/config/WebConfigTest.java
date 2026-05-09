package com.ebookstore.config;

import com.ebookstore.interceptor.AdminInterceptor;
import com.ebookstore.interceptor.LoginInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WebConfigTest {

    @Test
    void shouldRegisterInterceptorsAndCorsMappings() {
        WebConfig webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "loginInterceptor", new LoginInterceptor());
        ReflectionTestUtils.setField(webConfig, "adminInterceptor", new AdminInterceptor());

        assertDoesNotThrow(() -> webConfig.addInterceptors(new InterceptorRegistry()));
        assertDoesNotThrow(() -> webConfig.addCorsMappings(new CorsRegistry()));
    }
}
