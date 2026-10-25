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
        
        // TODO: Conectar con Appointment API para leer citas reales.
        // Por ahora, simulamos una cita a 24 horas.
        simulateReminder("123e4567-e89b-12d3-a456-426614174000", "pat-123", "prof-456", "PT24H");
    }

    private void simulateReminder(String appointmentId, String patientId, String professionalId, String offset) {
        try {
            // eventId determinista para evitar duplicados
            String eventId = UUID.nameUUIDFromBytes((appointmentId + offset).getBytes()).toString();
            
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
                "correlationId", "",
                "version", 1,
                "payload", payload
            );

            String json = objectMapper.writeValueAsString(envelope);
            
            rabbitTemplate.convertAndSend("appointments.events", "appointment-reminder-due", json, message -> {
                message.getMessageProperties().setMessageId(eventId);
                message.getMessageProperties().setType("AppointmentReminderDue");
                return message;
            });

            System.out.println("Published reminder: " + eventId);
        } catch (Exception e) {
            System.err.println("Failed to send reminder: " + e.getMessage());
        }
    }
}
