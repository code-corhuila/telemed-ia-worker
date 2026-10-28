package co.edu.corhuila.telemed.worker.application.usecase;

import co.edu.corhuila.telemed.worker.application.port.in.EventPayload;
import co.edu.corhuila.telemed.worker.application.port.out.DocumentClientPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProcessSummaryUseCaseTest {

    @Test
    void shouldCallDocumentClientWithCorrectParameters() {
        // Arrange
        DocumentClientPort mockClient = mock(DocumentClientPort.class);
        ProcessSummaryUseCase useCase = new ProcessSummaryUseCase(mockClient);
        EventPayload payload = new EventPayload(
                "sum-1", "cons-1", "pat-1", "corr-1"
        );

        // Act
        useCase.process("event-123", payload);

        // Assert
        verify(mockClient, times(1))
                .generatePdf("sum-1", "pat-1", "event-123", "corr-1");
    }

    @Test
    void shouldPropagateCorrelationIdToDocumentClient() {
        // Arrange
        DocumentClientPort mockClient = mock(DocumentClientPort.class);
        ProcessSummaryUseCase useCase = new ProcessSummaryUseCase(mockClient);
        EventPayload payload = new EventPayload(
                "sum-2", "cons-2", "pat-2", "corr-xyz"
        );

        // Act
        useCase.process("event-xyz", payload);

        // Assert
        ArgumentCaptor<String> correlationCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockClient).generatePdf(
                eq("sum-2"),
                eq("pat-2"),
                eq("event-xyz"),
                correlationCaptor.capture()
        );
        assertEquals("corr-xyz", correlationCaptor.getValue());
    }
}
