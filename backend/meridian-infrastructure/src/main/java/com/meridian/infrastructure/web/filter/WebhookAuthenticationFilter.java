package com.meridian.infrastructure.web.filter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class WebhookAuthenticationFilter extends OncePerRequestFilter {

    private static final String WEBHOOK_SECRET;
    private static final String WEBHOOK_PATH = "/api/v1/webhooks/documents";

    static {
        String secret = System.getenv("WEBHOOK_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("WEBHOOK_SECRET environment variable must be set");
        }
        WEBHOOK_SECRET = secret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (WEBHOOK_PATH.equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
            String signature = request.getHeader("X-HMAC-Signature");
            if (signature == null || signature.isBlank()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Missing HMAC signature");
                return;
            }

            String payload = request.getReader().lines()
                    .reduce("", (acc, line) -> acc + line);

            if (!verifyHmac(signature, payload)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Invalid HMAC signature");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean verifyHmac(String signature, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(WEBHOOK_SECRET.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] expected = mac.doFinal(payload.getBytes());
            String expectedHex = bytesToHex(expected);
            return signature.equals(expectedHex);
        } catch (Exception e) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}