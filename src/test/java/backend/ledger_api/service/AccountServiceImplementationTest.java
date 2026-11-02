package backend.ledger_api.service;

import backend.ledger_api.dto.CreateAccountRequest;
import backend.ledger_api.exception.AccountRequestException;
import backend.ledger_api.repository.AccountRepository;
import backend.ledger_api.repository.IdempotencyRecordRepository;
import backend.ledger_api.service.api.AccountServiceImplementation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceImplementationTest {
    private AccountRepository accounts;
    private IdempotencyRecordRepository idempotency;
    private AccountServiceImplementation service;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        idempotency = mock(IdempotencyRecordRepository.class);
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        service = new AccountServiceImplementation(accounts, idempotency, objectMapper);
    }

    @Test
    void rejectsUnsupportedCurrencyBeforeDatabaseAccess() {
        var request = new CreateAccountRequest("GBP", BigDecimal.ZERO);
        var error = assertThrows(AccountRequestException.class, () -> service.openAccount(request, "account-key"));
        assertEquals("account.currency.unsupported", error.getMessage());
        verifyNoInteractions(accounts, idempotency);
    }

    @Test
    void rejectsFractionalYenBeforeDatabaseAccess() {
        var request = new CreateAccountRequest("JPY", new BigDecimal("10.50"));
        var error = assertThrows(AccountRequestException.class, () -> service.openAccount(request, "account-key"));
        assertEquals("account.overdraft.invalid", error.getMessage());
        verifyNoInteractions(accounts, idempotency);
    }

    @Test
    void rejectsMissingIdempotencyKeyBeforeDatabaseAccess() {
        var request = new CreateAccountRequest("EUR", BigDecimal.ZERO);
        var error = assertThrows(AccountRequestException.class, () -> service.openAccount(request, " "));
        assertEquals("idempotency.key.required", error.getMessage());
        verifyNoInteractions(accounts, idempotency);
    }
}
