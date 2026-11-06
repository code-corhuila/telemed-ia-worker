package co.edu.corhuila.telemed.worker.adapter.in.dlq;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Replays messages from the DLQ back to the main queue every 5 minutes.
 * Messages that already have 3 x-death entries are dropped (poison pill)
 * to avoid an infinite loop.
 */
@Component
public class DlqReplay {

    private static final String DLQ = "post-consultation-summary-queue.dlq";
    private static final String MAIN = "post-consultation-summary-queue";
    private static final int MAX_REPLAYS = 3;

    private final RabbitTemplate rabbitTemplate;

    public DlqReplay(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 300000) // 5 min
    public void replay() {
        int replayed = 0;
        int dropped = 0;
        while (true) {
            Message msg = rabbitTemplate.receive(DLQ, 1000);
            if (msg == null) break;

            int deaths = deathCount(msg);
            if (deaths >= MAX_REPLAYS) {
                System.err.println("Dropping poison message after " + deaths + " deaths: " + msg.getMessageProperties().getMessageId());
                dropped++;
                continue;
            }

            rabbitTemplate.send("", MAIN, msg);
            System.out.println("Replayed DLQ message: " + msg.getMessageProperties().getMessageId());
            replayed++;
        }
        if (replayed > 0 || dropped > 0) {
            System.out.println("DLQ replay: replayed=" + replayed + " dropped=" + dropped);
        }
    }

    private int deathCount(Message msg) {
        Object xDeath = msg.getMessageProperties().getHeaders().get("x-death");
        if (!(xDeath instanceof java.util.List<?> list) || list.isEmpty()) {
            return 0;
        }
        Object first = list.get(0);
        if (first instanceof Map<?, ?> map) {
            Object count = map.get("count");
            if (count instanceof Long l) return l.intValue();
            if (count instanceof Integer i) return i;
        }
        return 0;
    }
}
