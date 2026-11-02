package backend.ledger_api.dto;

import backend.ledger_api.enums.EntryDirection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecordTransactionRequest(
        @NotBlank String description,
        @NotEmpty List<@Valid @NotNull EntryRequest> entries) {
    public record EntryRequest(
            @NotNull java.util.UUID accountId,
            @NotNull EntryDirection direction,
            @NotNull @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = false) java.math.BigDecimal amount) {
    }
}
