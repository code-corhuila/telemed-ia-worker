package co.edu.corhuila.telemed.worker.adapter.out.identity;

import co.edu.corhuila.telemed.worker.application.port.out.TokenProviderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class IdentityClient implements TokenProviderPort {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public IdentityClient(@Value("${worker.identity.url}") String identityUrl,
                          @Value("${worker.identity.client-id}") String clientId,
                          @Value("${worker.identity.client-secret}") String clientSecret) {
        this.restClient = RestClient.builder().baseUrl(identityUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getServiceToken() {
        // TODO: En un entorno real, se debería cachear el token hasta que expire
        Map<String, String> request = Map.of(
                "clientId", this.clientId,
                "clientSecret", this.clientSecret // En prod, esto viene de variables de entorno
        );

        TokenResponse response = restClient.post()
                .uri("/internal/service-token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new RuntimeException("Failed to obtain service token");
        }
        return response.accessToken();
    }

    private record TokenResponse(String accessToken) {}
}
