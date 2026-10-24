package co.edu.corhuila.telemed.worker.adapter.out.document;

import co.edu.corhuila.telemed.worker.application.port.out.DocumentClientPort;
import co.edu.corhuila.telemed.worker.application.port.out.TokenProviderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class DocumentClient implements DocumentClientPort {

    private final RestClient restClient;
    private final TokenProviderPort tokenProvider;

    public DocumentClient(@Value("${worker.document.url}") String documentUrl, TokenProviderPort tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(documentUrl).build();
        this.tokenProvider = tokenProvider;
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
                    .uri("/api/v1/documents/generate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity(); // Solo nos importa el status (2xx)
            
            System.out.println("Document API responded successfully for summary " + summaryId);
        } catch (Exception e) {
            System.err.println("Error calling Document API: " + e.getMessage());
            throw e; // Lanzamos la excepción para que RabbitMQ reintente (si es 5xx)
        }
    }
}
