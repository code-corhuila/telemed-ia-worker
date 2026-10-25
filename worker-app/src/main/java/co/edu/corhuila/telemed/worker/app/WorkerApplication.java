package co.edu.corhuila.telemed.worker.app;

import org.springframework.amqp.core.*;
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

    // --- Configuración de RabbitMQ Estándar (ADR-011) ---

    @Bean
    public TopicExchange consultationExchange() {
        return new TopicExchange("consultation.events");
    }

    @Bean
    public Queue summaryQueue() {
        return QueueBuilder.durable("post-consultation-summary-queue")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "post-consultation-summary-queue.dlq")
                .build();
    }

    @Bean
    public Binding summaryBinding(Queue summaryQueue, TopicExchange consultationExchange) {
        return BindingBuilder.bind(summaryQueue).to(consultationExchange).with("post-consultation-summary-generated");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("post-consultation-summary-queue.dlq").build();
    }
}
