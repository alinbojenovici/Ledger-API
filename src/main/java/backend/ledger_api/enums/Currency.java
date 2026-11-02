package backend.ledger_api.enums;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Currency {
    EUR(2),
    USD(2),
    JPY(0);

    private final int minorUnitScale;

    Currency(int minorUnitScale) {
        this.minorUnitScale = minorUnitScale;
    }

    public int minorUnitScale() {
        return minorUnitScale;
    }

    public long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(minorUnitScale).longValueExact();
    }

    public BigDecimal fromMinorUnits(long amount) {
        return BigDecimal.valueOf(amount, minorUnitScale);
    }

    public static Optional<Currency> find(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(currency -> currency.name().equals(code.trim().toUpperCase(Locale.ROOT)))
                .findFirst();
    }
}
