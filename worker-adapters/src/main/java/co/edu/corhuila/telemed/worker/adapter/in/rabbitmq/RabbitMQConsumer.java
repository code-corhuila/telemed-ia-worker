package co.edu.corhuila.telemed.worker.adapter.in.rabbitmq;

import co.edu.corhuila.telemed.worker.application.port.in.EventPayload;
import co.edu.corhuila.telemed.worker.application.usecase.ProcessSummaryUseCase;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;

@Component
public class RabbitMQConsumer {

    private final ProcessSummaryUseCase useCase;
    private final ObjectMapper objectMapper;

    public RabbitMQConsumer(ProcessSummaryUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "post-consultation-summary-queue")
    public void receiveMessage(Message message) {
        String amqpEventId = message.getMessageProperties().getMessageId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        
        try {
            // Extraer el payload interno del envoltorio ADR-011
            JsonNode rootNode = objectMapper.readTree(body);
            String realEventId = rootNode.path("eventId").asText(amqpEventId);
            JsonNode innerPayload = rootNode.path("payload");
            
            EventPayload payload = objectMapper.treeToValue(innerPayload, EventPayload.class);
            useCase.process(realEventId, payload);
        } catch (Exception e) {
            System.err.println("Error processing message " + amqpEventId + ": " + e.getMessage());
            // Al lanzar la excepción, Spring Boot hace el NACK y lo manda a la DLQ
            throw new RuntimeException("Failed to process event", e);
        }
    }
}
