package co.edu.corhuila.telemed.worker.adapter.out.appointment;

import co.edu.corhuila.telemed.worker.application.port.out.TokenProviderPort;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class AppointmentClient {

    private final RestClient restClient;
    private final TokenProviderPort tokenProvider;

    public AppointmentClient(@Value("${worker.appointment.url}") String appointmentUrl, TokenProviderPort tokenProvider) {
        this.tokenProvider = tokenProvider;

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder().baseUrl(appointmentUrl).requestFactory(factory).build();
    }

    public JsonNode getUpcomingReminders() {
        String token = tokenProvider.getServiceToken();
        return restClient.get()
                .uri("/api/appointments/internal/reminders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(JsonNode.class);
    }
}
