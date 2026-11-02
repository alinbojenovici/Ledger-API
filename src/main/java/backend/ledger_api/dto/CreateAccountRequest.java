package backend.ledger_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank String currency,
        @PositiveOrZero BigDecimal overdraftLimit) {
}
