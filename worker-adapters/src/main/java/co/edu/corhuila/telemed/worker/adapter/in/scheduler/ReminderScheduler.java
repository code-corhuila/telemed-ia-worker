package co.edu.corhuila.telemed.worker.adapter.in.scheduler;

import co.edu.corhuila.telemed.worker.adapter.out.appointment.AppointmentClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReminderScheduler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final AppointmentClient appointmentClient;

    // Memoria en RAM para no enviar el mismo recordatorio dos veces
    private final Set<String> sentEvents = ConcurrentHashMap.newKeySet();

    public ReminderScheduler(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, AppointmentClient appointmentClient) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.appointmentClient = appointmentClient;
    }

    @Scheduled(fixedDelay = 60000) // Ejecutar cada minuto
    public void checkUpcomingAppointments() {
        System.out.println("Checking upcoming appointments for reminders...");
        
        try {
            JsonNode appointments = appointmentClient.getUpcomingReminders();
            
            if (appointments == null || !appointments.isArray()) {
                System.out.println("No appointments to process or invalid response.");
                return;
            }

            String jobCorrelationId = UUID.randomUUID().toString();

            for (JsonNode appt : appointments) {
                String appointmentId = appt.path("id").asText();
                String patientId = appt.path("patientId").asText();
                String professionalId = appt.path("professionalId").asText();
                String startDateTime = appt.path("start").asText();
                String offset = "PT24H";

                String eventId = UUID.nameUUIDFromBytes((appointmentId + offset).getBytes()).toString();

                if (sentEvents.contains(eventId)) {
                    continue;
                }

                publishReminder(eventId, jobCorrelationId, appointmentId, patientId, professionalId, startDateTime, offset);
                sentEvents.add(eventId);
            }
        } catch (Exception e) {
            System.err.println("Error checking upcoming appointments: " + e.getMessage());
        }
    }

    private void publishReminder(String eventId, String correlationId, String appointmentId, 
                                 String patientId, String professionalId, String startDateTime, String offset) {
        try {
            Map<String, Object> payload = Map.of(
                "appointmentId", appointmentId,
                "patientId", patientId,
                "professionalId", professionalId,
                "startDateTime", startDateTime,
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

            System.out.println("Published real reminder for appointment: " + appointmentId);
        } catch (Exception e) {
            System.err.println("Failed to send reminder: " + e.getMessage());
        }
    }
}
