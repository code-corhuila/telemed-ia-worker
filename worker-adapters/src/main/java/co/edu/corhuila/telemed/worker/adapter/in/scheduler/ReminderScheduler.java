package co.edu.corhuila.telemed.worker.adapter.in.scheduler;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class ReminderScheduler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public ReminderScheduler(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 60000) // Ejecutar cada minuto
    public void checkUpcomingAppointments() {
        System.out.println("Checking upcoming appointments for reminders...");
        
        // Simulamos una cita real (UUIDs válidos para no ir a la DLQ)
        String appointmentId = UUID.randomUUID().toString();
        String patientId = UUID.randomUUID().toString();
        String professionalId = UUID.randomUUID().toString();
        
        simulateReminder(appointmentId, patientId, professionalId, "PT24H");
    }

    private void simulateReminder(String appointmentId, String patientId, String professionalId, String offset) {
        try {
            // eventId determinista (UUID válido)
            String eventId = UUID.nameUUIDFromBytes((appointmentId + offset).getBytes()).toString();
            // correlationId real para trazabilidad
            String correlationId = UUID.randomUUID().toString();

            Map<String, Object> payload = Map.of(
                "appointmentId", appointmentId,
                "patientId", patientId,
                "professionalId", professionalId,
                "startDateTime", Instant.now().plusSeconds(86400).toString(),
                "reminderOffset", offset
            );

            Map<String, Object> envelope = Map.of(
                "eventId", eventId,
                "eventType", "AppointmentReminderDue",
                "occurredAt", Instant.now().toString(),
                "correlationId", correlationId,
                "version", 1,
                "payload", payload
            );

            String json = objectMapper.writeValueAsString(envelope);
            
            rabbitTemplate.convertAndSend("appointments.events", "appointment-reminder-due", json, message -> {
                message.getMessageProperties().setMessageId(eventId);
                message.getMessageProperties().setType("AppointmentReminderDue");
                message.getMessageProperties().setCorrelationId(correlationId);
                return message;
            });

            System.out.println("Published reminder: " + eventId);
        } catch (Exception e) {
            System.err.println("Failed to send reminder: " + e.getMessage());
        }
    }
}
