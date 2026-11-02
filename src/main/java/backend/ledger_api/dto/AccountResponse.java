package backend.ledger_api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(UUID id, String currency, BigDecimal overdraftLimit, BigDecimal balance,
                              Instant createdAt) {
}
