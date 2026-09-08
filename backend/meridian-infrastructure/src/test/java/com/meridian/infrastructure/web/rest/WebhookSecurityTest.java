package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class, com.meridian.infrastructure.web.filter.WebhookAuthenticationFilter.class})
class WebhookSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngestDocumentUseCase ingestDocumentUseCase;

    @Test
    void shouldBypassWebhookFilterWithDifferentCase() throws Exception {
        String payload = "{\"type\":\"INVOICE\",\"content\":\"test\"}";

        mockMvc.perform(post("/API/V1/WEBHOOKS/DOCUMENTS")
                .header("X-HMAC-Signature", "invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectWebhookWithMissingSignature() throws Exception {
        String payload = "{\"type\":\"INVOICE\",\"content\":\"test\"}";

        mockMvc.perform(post("/api/v1/webhooks/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWebhookWithInvalidSignature() throws Exception {
        String payload = "{\"type\":\"INVOICE\",\"content\":\"test\"}";

        mockMvc.perform(post("/api/v1/webhooks/documents")
                .header("X-HMAC-Signature", "invalid-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptWebhookWithValidSignature() throws Exception {
        String payload = "{\"type\":\"INVOICE\",\"content\":\"test\"}";
        String validSignature = computeHmac(payload);

        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of(), null);
        when(ingestDocumentUseCase.ingest(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(document);

        mockMvc.perform(post("/api/v1/webhooks/documents")
                .header("X-HMAC-Signature", validSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());
    }

    private String computeHmac(String payload) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    "changeme-webhook-secret".getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] expected = mac.doFinal(payload.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : expected) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
