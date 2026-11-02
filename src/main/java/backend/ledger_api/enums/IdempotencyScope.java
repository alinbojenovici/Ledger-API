package backend.ledger_api.enums;

public enum IdempotencyScope {
    ACCOUNT_CREATION,
    TRANSACTION_RECORDING,
    TRANSACTION_REVERSAL
}
