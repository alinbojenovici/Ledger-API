package backend.ledger_api.dto;

import backend.ledger_api.enums.EntryDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(UUID id, String description, Instant recordedAt, UUID reversalOf, List<EntryResponse> entries) {
    public record EntryResponse(UUID accountId, EntryDirection direction, BigDecimal amount) {
    }
}
