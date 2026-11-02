package backend.ledger_api.service;

import backend.ledger_api.dto.RecordTransactionRequest;
import backend.ledger_api.dto.TransactionResponse;

import java.util.UUID;

public interface TransactionService {
    TransactionResponse recordTransaction(RecordTransactionRequest request, String idempotencyKey);

    TransactionResponse getTransaction(UUID transactionId);

    TransactionResponse reverseTransaction(UUID transactionId, String idempotencyKey);

}
