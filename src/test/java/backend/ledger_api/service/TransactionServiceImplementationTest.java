package backend.ledger_api.service;

import backend.ledger_api.dto.RecordTransactionRequest;
import backend.ledger_api.entity.Account;
import backend.ledger_api.entity.IdempotencyRecord;
import backend.ledger_api.enums.EntryDirection;
import backend.ledger_api.exception.AccountConflictException;
import backend.ledger_api.exception.AccountRequestException;
import backend.ledger_api.repository.AccountRepository;
import backend.ledger_api.repository.IdempotencyRecordRepository;
import backend.ledger_api.repository.LedgerTransactionRepository;
import backend.ledger_api.service.api.TransactionServiceImplementation;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceImplementationTest {
    private AccountRepository accounts;
    private LedgerTransactionRepository transactions;
    private IdempotencyRecordRepository idempotency;
    private TransactionServiceImplementation service;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        transactions = mock(LedgerTransactionRepository.class);
        idempotency = mock(IdempotencyRecordRepository.class);
        service = new TransactionServiceImplementation(accounts, transactions, idempotency, new ObjectMapper());
        accountId = UUID.randomUUID();
    }

    @Test
    void rejectsLessThanTwoEntries() {
        var request = new RecordTransactionRequest("invalid", List.of(new RecordTransactionRequest.EntryRequest(accountId, EntryDirection.DEBIT, new BigDecimal("1.00"))));
        var error = assertThrows(AccountRequestException.class, () -> service.recordTransaction(request, "key"));
        assertEquals("transaction.entries.minimum", error.getMessage());
        verifyNoInteractions(accounts, transactions);
    }

    @Test
    void rejectsUnbalancedEntries() {
        Account account = new Account("EUR", 0);
        setId(account, accountId);
        when(accounts.findAndLockAllByIdOrdered(List.of(accountId))).thenReturn(List.of(account));
        var request = new RecordTransactionRequest("unbalanced", List.of(
                new RecordTransactionRequest.EntryRequest(accountId, EntryDirection.DEBIT, new BigDecimal("1.00")),
                new RecordTransactionRequest.EntryRequest(accountId, EntryDirection.CREDIT, new BigDecimal("2.00"))));
        var error = assertThrows(AccountRequestException.class, () -> service.recordTransaction(request, "key"));
        assertEquals("transaction.not.balanced", error.getMessage());
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void rejectsOverdraftWithoutSavingTransaction() {
        UUID secondId = UUID.randomUUID();
        Account debitAccount = new Account("EUR", 0);
        setId(debitAccount, accountId);
        Account creditAccount = new Account("EUR", 0);
        setId(creditAccount, secondId);
        when(accounts.findAndLockAllByIdOrdered(anyList())).thenReturn(List.of(debitAccount, creditAccount));
        when(idempotency.findByScopeAndIdempotencyKey(anyString(), anyString())).thenReturn(Optional.empty());
        var request = new RecordTransactionRequest("overdraft", List.of(
                new RecordTransactionRequest.EntryRequest(accountId, EntryDirection.DEBIT, new BigDecimal("1.00")),
                new RecordTransactionRequest.EntryRequest(secondId, EntryDirection.CREDIT, new BigDecimal("1.00"))));
        assertThrows(AccountConflictException.class, () -> service.recordTransaction(request, "key"));
        assertEquals(0, debitAccount.getBalanceMinor());
        verify(transactions, never()).saveAndFlush(any());
    }

    private static void setId(Account account, UUID id) {
        try {
            var field = Account.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
