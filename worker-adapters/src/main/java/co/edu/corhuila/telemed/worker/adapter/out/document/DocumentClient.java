package co.edu.corhuila.telemed.worker.adapter.out.document;

import co.edu.corhuila.telemed.worker.application.port.out.DocumentClientPort;
import co.edu.corhuila.telemed.worker.application.port.out.TokenProviderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class DocumentClient implements DocumentClientPort {

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
    public void generatePdf(String summaryId, String patientId, String idempotencyKey) {
        String token = tokenProvider.getServiceToken();

        Map<String, String> body = Map.of(
                "summaryId", summaryId,
                "patientId", patientId
        );

        try {
            restClient.post()
                    .uri("/api/documents/generate") // ¡Ojo! Sin el /v1
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            
            System.out.println("Document API responded successfully for summary " + summaryId);
        } catch (Exception e) {
            System.err.println("Error calling Document API: " + e.getMessage());
            throw e;
        }
    }
}
