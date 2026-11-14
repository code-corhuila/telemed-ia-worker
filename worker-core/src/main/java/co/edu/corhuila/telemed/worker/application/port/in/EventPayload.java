package co.edu.corhuila.telemed.worker.application.port.in;

/**
 * Payload of the PostConsultationSummaryGenerated event.
 *
 * correlationId is the request-scoped X-Correlation-Id propagated
 * by the producer (Medical Consultation API) and stored in the event envelope.
 * It is used to correlate the consumer logs with the HTTP request that
 * generated the summary.
 */
public record EventPayload(
    String summaryId,
    String consultationId,
    String patientId,
    String correlationId
) {}
