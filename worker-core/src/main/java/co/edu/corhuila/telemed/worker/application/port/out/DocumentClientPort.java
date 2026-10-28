package co.edu.corhuila.telemed.worker.application.port.out;

public interface DocumentClientPort {
    void generatePdf(String summaryId, String patientId, String idempotencyKey, String correlationId);
}
