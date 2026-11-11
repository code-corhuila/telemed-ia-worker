package co.edu.corhuila.telemed.worker.application.port.in;

public record EventPayload(
    String summaryId,
    String consultationId,
    String patientId
) {}
