package backend.ledger_api.service;

import backend.ledger_api.dto.AccountResponse;
import backend.ledger_api.dto.CreateAccountRequest;

import java.util.UUID;

public interface AccountService {
    AccountResponse openAccount(CreateAccountRequest request, String idempotencyKey);

    AccountResponse getAccount(UUID accountId);
}
