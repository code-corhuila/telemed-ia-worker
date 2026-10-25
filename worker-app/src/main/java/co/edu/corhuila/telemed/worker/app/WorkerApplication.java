package co.edu.corhuila.telemed.worker.app;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import co.edu.corhuila.telemed.worker.application.usecase.ProcessSummaryUseCase;
import co.edu.corhuila.telemed.worker.application.port.out.DocumentClientPort;

@SpringBootApplication(scanBasePackages = "co.edu.corhuila.telemed.worker")
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

    @Bean
    public ProcessSummaryUseCase processSummaryUseCase(DocumentClientPort documentClient) {
        return new ProcessSummaryUseCase(documentClient);
    }

    // Declarar la cola para que Spring la cree si no existe
    @Bean
    public Queue summaryQueue() {
        return QueueBuilder.durable("post_consultation_summary_generated")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "post_consultation_summary_generated.dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("post_consultation_summary_generated.dlq").build();
    }
}
