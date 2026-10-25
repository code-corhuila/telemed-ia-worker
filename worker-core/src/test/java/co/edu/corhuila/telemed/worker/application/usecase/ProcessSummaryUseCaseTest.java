package co.edu.corhuila.telemed.worker.application.usecase;

import co.edu.corhuila.telemed.worker.application.port.in.EventPayload;
import co.edu.corhuila.telemed.worker.application.port.out.DocumentClientPort;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ProcessSummaryUseCaseTest {

    @Test
    void shouldCallDocumentClientWithCorrectParameters() {
        // Arrange
        DocumentClientPort mockClient = mock(DocumentClientPort.class);
        ProcessSummaryUseCase useCase = new ProcessSummaryUseCase(mockClient);
        EventPayload payload = new EventPayload("sum-1", "cons-1", "pat-1");

        // Act
        useCase.process("event-123", payload);

        // Assert
        verify(mockClient, times(1)).generatePdf("sum-1", "pat-1", "event-123");
    }
}
