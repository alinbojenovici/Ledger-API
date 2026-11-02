package backend.ledger_api;

import backend.ledger_api.dto.CreateAccountRequest;
import backend.ledger_api.dto.RecordTransactionRequest;
import backend.ledger_api.enums.EntryDirection;
import backend.ledger_api.exception.AccountConflictException;
import backend.ledger_api.repository.LedgerEntryRepository;
import backend.ledger_api.repository.LedgerTransactionRepository;
import backend.ledger_api.service.AccountService;
import backend.ledger_api.service.TransactionService;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class LedgerMySqlIntegrationTest {
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4")
            .withDatabaseName("LEDGER_TRANSACTIONS").withUsername("ledger").withPassword("ledger");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    AccountService accountService;
    @Autowired
    TransactionService transactionService;
    @Autowired
    LedgerTransactionRepository transactionRepository;
    @Autowired
    LedgerEntryRepository entryRepository;

    @Test
    void recordsAndReversesTransactionInMySql() {
        var debitAccount = accountService.openAccount(new CreateAccountRequest("EUR", new BigDecimal("200.00")), "it-account-debit");
        var creditAccount = accountService.openAccount(new CreateAccountRequest("EUR", BigDecimal.ZERO), "it-account-credit");
        var request = new RecordTransactionRequest("Integration test transfer", List.of(
                new RecordTransactionRequest.EntryRequest(debitAccount.id(), EntryDirection.DEBIT, new BigDecimal("25.50")),
                new RecordTransactionRequest.EntryRequest(creditAccount.id(), EntryDirection.CREDIT, new BigDecimal("25.50"))));

        var recorded = transactionService.recordTransaction(request, "it-transaction");
        assertEquals(2, recorded.entries().size());
        assertEquals(new BigDecimal("-25.50"), accountService.getAccount(debitAccount.id()).balance());
        assertEquals(new BigDecimal("25.50"), accountService.getAccount(creditAccount.id()).balance());

        var reversal = transactionService.reverseTransaction(recorded.id(), "it-reversal");
        assertNotEquals(recorded.id(), reversal.id());
        assertEquals(recorded.id(), reversal.reversalOf());
        assertEquals(EntryDirection.CREDIT, reversal.entries().get(0).direction());
        assertEquals(EntryDirection.DEBIT, reversal.entries().get(1).direction());
        assertEquals(BigDecimal.ZERO.setScale(2), accountService.getAccount(debitAccount.id()).balance());
        assertEquals(BigDecimal.ZERO.setScale(2), accountService.getAccount(creditAccount.id()).balance());
    }

    @Test
    void refusedOverdraftLeavesBalancesTransactionsAndEntriesUnchangedInMySql() {
        String suffix = java.util.UUID.randomUUID().toString();
        var debitAccount = accountService.openAccount(new CreateAccountRequest("EUR", BigDecimal.ZERO), "it-refusal-debit-" + suffix);
        var creditAccount = accountService.openAccount(new CreateAccountRequest("EUR", BigDecimal.ZERO), "it-refusal-credit-" + suffix);
        long transactionCountBefore = transactionRepository.count();
        long entryCountBefore = entryRepository.count();
        var request = new RecordTransactionRequest("Overdraft refusal test", List.of(
                new RecordTransactionRequest.EntryRequest(debitAccount.id(), EntryDirection.DEBIT, new BigDecimal("10.00")),
                new RecordTransactionRequest.EntryRequest(creditAccount.id(), EntryDirection.CREDIT, new BigDecimal("10.00"))));

        assertThrows(AccountConflictException.class,
                () -> transactionService.recordTransaction(request, "it-refused-transaction-" + suffix));

        assertEquals(BigDecimal.ZERO.setScale(2), accountService.getAccount(debitAccount.id()).balance());
        assertEquals(BigDecimal.ZERO.setScale(2), accountService.getAccount(creditAccount.id()).balance());
        assertEquals(transactionCountBefore, transactionRepository.count());
        assertEquals(entryCountBefore, entryRepository.count());
    }
}
