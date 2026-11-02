package backend.ledger_api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "LA_IDEMPOTENCY_RECORD", uniqueConstraints = @UniqueConstraint(name = "UQ_LA_IDEMPOTENCY_SCOPE_KEY", columnNames = {"SCOPE", "IDEMPOTENCY_KEY"}))
public class IdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "SCOPE", nullable = false, length = 32)
    private String scope;

    @Column(name = "IDEMPOTENCY_KEY", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "REQUEST_HASH", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "RESOURCE_ID")
    private UUID resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "RESPONSE_BODY", columnDefinition = "json")
    private String responseBody;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String scope, String key, String hash, UUID resourceId, String responseBody) {
        this.scope = scope;
        this.idempotencyKey = key;
        this.requestHash = hash;
        this.resourceId = resourceId;
        this.responseBody = responseBody;
        this.createdAt = Instant.now();
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
