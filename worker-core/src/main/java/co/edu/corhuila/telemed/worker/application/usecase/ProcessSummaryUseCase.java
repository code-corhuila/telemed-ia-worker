package co.edu.corhuila.telemed.worker.application.usecase;

import co.edu.corhuila.telemed.worker.application.port.in.EventPayload;
import co.edu.corhuila.telemed.worker.application.port.out.DocumentClientPort;

public class ProcessSummaryUseCase {
    
    private final DocumentClientPort documentClient;

    public ProcessSummaryUseCase(DocumentClientPort documentClient) {
        this.documentClient = documentClient;
    }

    public void process(String eventId, EventPayload payload) {
        System.out.println("Processing event " + eventId + " for summary " + payload.summaryId());
        
        // El eventId actúa como Idempotency-Key
        documentClient.generatePdf(payload.summaryId(), payload.patientId(), eventId);
        
        System.out.println("Successfully triggered PDF generation for summary " + payload.summaryId());
    }
}
