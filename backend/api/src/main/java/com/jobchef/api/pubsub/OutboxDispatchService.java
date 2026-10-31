package com.jobchef.api.pubsub;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.jobchef.shared.config.BackendProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxDispatchService {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxDispatchService.class);
    private final JdbcTemplate jdbc;
    private final Publisher publisher;
    private final BackendProperties properties;

    public OutboxDispatchService(
            JdbcTemplate jdbc, Publisher publisher, BackendProperties properties) {
        this.jdbc = jdbc;
        this.publisher = publisher;
        this.properties = properties;
    }

    @Transactional
    public DispatchResult dispatch() {
        jdbc.update("DELETE FROM outbox WHERE published_at < now() - interval '7 days'");
        int limit = Math.max(1, Math.min(properties.limits().maxOutboxBatchSize(), 100));
        List<OutboxEvent> events = jdbc.query("""
                SELECT id, aggregate_id, payload::text AS payload
                FROM outbox
                WHERE published_at IS NULL AND next_attempt_at <= now()
                ORDER BY created_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """, (rs, row) -> new OutboxEvent(
                rs.getObject("id", UUID.class),
                rs.getObject("aggregate_id", UUID.class),
                rs.getString("payload")), limit);
        int published = 0;
        int failed = 0;
        for (OutboxEvent event : events) {
            try {
                PubsubMessage message = PubsubMessage.newBuilder()
                        .setData(ByteString.copyFromUtf8(event.payload()))
                        .putAttributes("outboxId", event.id().toString())
                        .putAttributes("applicationId", event.aggregateId().toString())
                        .build();
                publisher.publish(message).get(10, TimeUnit.SECONDS);
                jdbc.update("""
                        UPDATE outbox SET published_at = now(), attempts = attempts + 1
                        WHERE id = ? AND published_at IS NULL
                        """, event.id());
                published++;
            } catch (Exception exception) {
                jdbc.update("""
                        UPDATE outbox
                        SET attempts = attempts + 1,
                            next_attempt_at = now() + (? * interval '1 second')
                        WHERE id = ? AND published_at IS NULL
                        """, Math.min(3600, 30L * (1L << Math.min(7, attempts(event.id())))), event.id());
                LOG.warn("Outbox publication failed for event {}.", event.id());
                failed++;
            }
        }
        return new DispatchResult(events.size(), published, failed);
    }

    private int attempts(UUID id) {
        Integer value = jdbc.queryForObject("SELECT attempts FROM outbox WHERE id = ?", Integer.class, id);
        return value == null ? 0 : value;
    }

    public record DispatchResult(int examined, int published, int failed) {}
    private record OutboxEvent(UUID id, UUID aggregateId, String payload) {}
}
