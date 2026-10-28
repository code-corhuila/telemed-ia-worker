package co.edu.corhuila.telemed.worker.application.usecase;

import co.edu.corhuila.telemed.worker.application.port.in.EventPayload;
import co.edu.corhuila.telemed.worker.application.port.out.DocumentClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ProcessSummaryUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessSummaryUseCase.class);
    static final String MDC_CORRELATION_ID = "correlationId";

    private final DocumentClientPort documentClient;

    public ProcessSummaryUseCase(DocumentClientPort documentClient) {
        this.documentClient = documentClient;
    }

    public void process(String eventId, EventPayload payload) {
        boolean correlationWasSet = false;
        if (payload.correlationId() != null && !payload.correlationId().isBlank()) {
            MDC.put(MDC_CORRELATION_ID, payload.correlationId());
            correlationWasSet = true;
        }

        try {
            LOG.info("Processing event {} for summary {} (consultation {})",
                    eventId, payload.summaryId(), payload.consultationId());

            documentClient.generatePdf(
                    payload.summaryId(),
                    payload.patientId(),
                    eventId,
                    payload.correlationId()
            );

            LOG.info("Successfully triggered PDF generation for summary {}", payload.summaryId());
        } finally {
            if (correlationWasSet) {
                MDC.remove(MDC_CORRELATION_ID);
            }
        }
    }
}
