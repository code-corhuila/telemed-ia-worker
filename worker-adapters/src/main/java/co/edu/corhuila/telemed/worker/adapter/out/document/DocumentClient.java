package co.edu.corhuila.telemed.worker.adapter.out.document;

import co.edu.corhuila.telemed.worker.application.port.out.DocumentClientPort;
import co.edu.corhuila.telemed.worker.application.port.out.TokenProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

@Component
public class DocumentClient implements DocumentClientPort {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentClient.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final RestClient restClient;
    private final TokenProviderPort tokenProvider;

    public DocumentClient(@Value("${worker.document.url}") String documentUrl, TokenProviderPort tokenProvider) {
        this.tokenProvider = tokenProvider;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(documentUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void generatePdf(String summaryId, String patientId, String idempotencyKey, String correlationId) {
        String token = tokenProvider.getServiceToken();

        Map<String, String> body = Map.of(
                "summaryId", summaryId,
                "patientId", patientId
        );

        String traceparent = buildTraceparent();

        var request = restClient.post()
                .uri("/api/documents/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .header("traceparent", traceparent)
                .contentType(MediaType.APPLICATION_JSON);

        if (correlationId != null && !correlationId.isBlank()) {
            request = request.header("X-Correlation-Id", correlationId);
        }

        try {
            request.body(body).retrieve().toBodilessEntity();
            LOG.info("Document API responded successfully for summary {}", summaryId);
        } catch (Exception e) {
            LOG.error("Error calling Document API for summary {}: {}", summaryId, e.getMessage(), e);
            throw e;
        }
    }

    private String buildTraceparent() {
        String traceId = MDC.get("traceId");
        if (traceId == null || !traceId.matches("[0-9a-f]{32}")) {
            byte[] bytes = new byte[16];
            RANDOM.nextBytes(bytes);
            traceId = HEX.formatHex(bytes);
        }
        byte[] spanBytes = new byte[8];
        RANDOM.nextBytes(spanBytes);
        String spanId = HEX.formatHex(spanBytes);
        return "00-" + traceId + "-" + spanId + "-01";
    }
}
