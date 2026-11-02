package backend.ledger_api.service.api;

import backend.ledger_api.dto.AccountResponse;
import backend.ledger_api.dto.CreateAccountRequest;
import backend.ledger_api.entity.Account;
import backend.ledger_api.entity.IdempotencyRecord;
import backend.ledger_api.enums.Currency;
import backend.ledger_api.exception.AccountConflictException;
import backend.ledger_api.exception.AccountNotFoundException;
import backend.ledger_api.exception.AccountRequestException;
import backend.ledger_api.repository.AccountRepository;
import backend.ledger_api.repository.IdempotencyRecordRepository;
import backend.ledger_api.service.AccountService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImplementation implements AccountService {
    private static final backend.ledger_api.enums.IdempotencyScope IDEMPOTENCY_SCOPE = backend.ledger_api.enums.IdempotencyScope.ACCOUNT_CREATION;
    private final AccountRepository accounts;
    private final IdempotencyRecordRepository idempotency;
    private final ObjectMapper objectMapper;

    public AccountServiceImplementation(AccountRepository accounts, IdempotencyRecordRepository idempotency, ObjectMapper objectMapper) {
        this.accounts = accounts;
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AccountResponse openAccount(CreateAccountRequest request, String idempotencyKey) {
        if (request == null || request.currency() == null)
            throw new AccountRequestException("account.validation.required");

        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 255)
            throw new AccountRequestException("idempotency.key.required");

        String currencyCode = request.currency().trim().toUpperCase(Locale.ROOT);
        Currency currency = Currency.find(currencyCode)
                .orElseThrow(() -> new AccountRequestException("account.currency.unsupported", currencyCode));

        BigDecimal limit = request.overdraftLimit() == null ? BigDecimal.ZERO : request.overdraftLimit();
        int scale = currency.minorUnitScale();
        if (limit.signum() < 0 || limit.scale() > scale)
            throw new AccountRequestException("account.overdraft.invalid", currency.name(), scale);
        long minor;
        try {
            minor = currency.toMinorUnits(limit);
        } catch (ArithmeticException ex) {
            throw new AccountRequestException("account.overdraft.invalid", currency.name(), scale);
        }

        String hash = hash(currency.name() + "|" + minor);
        var previous = idempotency.findByScopeAndIdempotencyKey(IDEMPOTENCY_SCOPE.name(), idempotencyKey);
        if (previous.isPresent()) {
            IdempotencyRecord saved = previous.get();
            if (!saved.getRequestHash().equals(hash)) throw new AccountConflictException("idempotency.key.conflict");
            return readSavedResponse(saved.getResponseBody());
        }

        Account account = accounts.saveAndFlush(new Account(currency.name(), minor));
        AccountResponse response = toResponse(account);
        idempotency.save(new IdempotencyRecord(IDEMPOTENCY_SCOPE.name(), idempotencyKey, hash, account.getId(), serialize(response)));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        if (accountId == null) throw new AccountRequestException("account.id.invalid");

        Account account = accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException("account.not.found", accountId));
        return toResponse(account);
    }

    private AccountResponse toResponse(Account a) {
        Currency currency = Currency.find(a.getCurrency()).orElseThrow();
        return new AccountResponse(a.getId(), currency.name(), currency.fromMinorUnits(a.getOverdraftLimitMinor()), currency.fromMinorUnits(a.getBalanceMinor()), a.getCreatedAt());
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String serialize(AccountResponse value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private AccountResponse readSavedResponse(String value) {
        try {
            return objectMapper.readValue(value, AccountResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read saved idempotent response", e);
        }
    }
}
