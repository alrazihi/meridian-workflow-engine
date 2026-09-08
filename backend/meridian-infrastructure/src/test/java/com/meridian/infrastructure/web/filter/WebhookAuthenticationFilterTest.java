package com.meridian.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookAuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    void shouldPassThroughNonWebhookRequests() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/documents");
        when(request.getMethod()).thenReturn("GET");

        new WebhookAuthenticationFilter("test-secret").doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectWebhookWithMissingSignature() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/webhooks/documents");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-HMAC-Signature")).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        new WebhookAuthenticationFilter("test-secret").doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(filterChain, org.mockito.Mockito.never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectWebhookWithInvalidSignature() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/webhooks/documents");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-HMAC-Signature")).thenReturn("invalid-signature");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        new WebhookAuthenticationFilter("test-secret").doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(filterChain, org.mockito.Mockito.never()).doFilter(any(), any());
    }

    @Test
    void shouldPassThroughWebhookWithValidSignature() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/webhooks/documents");
        when(request.getMethod()).thenReturn("POST");

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec("test-secret".getBytes(), "HmacSHA256");
        mac.init(secretKey);
        byte[] expected = mac.doFinal("{\"type\":\"INVOICE\",\"content\":\"test\"}".getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : expected) {
            sb.append(String.format("%02x", b));
        }
        String validSignature = sb.toString();

        when(request.getHeader("X-HMAC-Signature")).thenReturn(validSignature);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        new WebhookAuthenticationFilter("test-secret").doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
