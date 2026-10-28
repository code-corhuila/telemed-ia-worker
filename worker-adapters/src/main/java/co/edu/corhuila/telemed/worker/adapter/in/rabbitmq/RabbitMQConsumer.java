package co.edu.corhuila.telemed.worker.adapter.in.rabbitmq;

import co.edu.corhuila.telemed.worker.application.port.in.EventPayload;
import co.edu.corhuila.telemed.worker.application.usecase.ProcessSummaryUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RabbitMQConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQConsumer.class);
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_TRACE_ID = "traceId";

    private final ProcessSummaryUseCase useCase;
    private final ObjectMapper objectMapper;

    public RabbitMQConsumer(ProcessSummaryUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "post-consultation-summary-queue")
    public void receiveMessage(Message message) {
        String amqpEventId = message.getMessageProperties().getMessageId();
        String amqpCorrelationId = message.getMessageProperties().getCorrelationId();
        String traceparent = (String) message.getMessageProperties().getHeaders().get("traceparent");
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        boolean correlationWasSet = false;
        boolean traceWasSet = false;

        try {
            JsonNode rootNode = objectMapper.readTree(body);
            String eventId = rootNode.path("eventId").asText(amqpEventId);

            String correlationId = rootNode.path("correlationId").asText(null);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = amqpCorrelationId;
            }
            if (correlationId != null && !correlationId.isBlank()) {
                MDC.put(MDC_CORRELATION_ID, correlationId);
                correlationWasSet = true;
            }

            if (traceparent != null) {
                String[] parts = traceparent.split("-");
                if (parts.length == 4 && parts[0].equals("00") && parts[1].matches("[0-9a-f]{32}")) {
                    MDC.put(MDC_TRACE_ID, parts[1]);
                    traceWasSet = true;
                }
            }

            JsonNode innerPayload = rootNode.path("payload");
            EventPayload payload = new EventPayload(
                    innerPayload.path("summaryId").asText(null),
                    innerPayload.path("consultationId").asText(null),
                    innerPayload.path("patientId").asText(null),
                    correlationId
            );

            LOG.info("Received PostConsultationSummaryGenerated event={} summary={}",
                    eventId, payload.summaryId());

            useCase.process(eventId, payload);
        } catch (Exception e) {
            LOG.error("Error processing message event={} correlation={}: {}",
                    amqpEventId, amqpCorrelationId, e.getMessage(), e);
            throw new RuntimeException("Failed to process event " + amqpEventId, e);
        } finally {
            if (correlationWasSet) {
                MDC.remove(MDC_CORRELATION_ID);
            }
            if (traceWasSet) {
                MDC.remove(MDC_TRACE_ID);
            }
        }
    }
}
