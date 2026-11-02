package backend.ledger_api.service.api;

import backend.ledger_api.dto.RecordTransactionRequest;
import backend.ledger_api.dto.TransactionResponse;
import backend.ledger_api.entity.*;
import backend.ledger_api.enums.Currency;
import backend.ledger_api.enums.IdempotencyScope;
import backend.ledger_api.exception.*;
import backend.ledger_api.repository.*;
import backend.ledger_api.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionServiceImplementation implements TransactionService {

    private static final IdempotencyScope IDEMPOTENCY_SCOPE = IdempotencyScope.TRANSACTION_RECORDING;
    private final AccountRepository accounts;
    private final LedgerTransactionRepository transactions;
    private final IdempotencyRecordRepository idempotency;
    private final ObjectMapper objectMapper;

    public TransactionServiceImplementation(AccountRepository accountRepository, LedgerTransactionRepository transactionRepository, IdempotencyRecordRepository idempotencyRecordRepository, ObjectMapper objectMapper) {
        this.accounts = accountRepository;
        this.transactions = transactionRepository;
        this.idempotency = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TransactionResponse recordTransaction(RecordTransactionRequest request, String key) {
        if (request == null || request.entries() == null || request.entries().size() < 2)
            throw new AccountRequestException("transaction.entries.minimum");

        if (key == null || key.isBlank() || key.length() > 255)
            throw new AccountRequestException("idempotency.key.required");

        List<UUID> accountIds = request.entries().stream().map(RecordTransactionRequest.EntryRequest::accountId).filter(Objects::nonNull).distinct().sorted().toList();

        if (accountIds.size() != request.entries().stream().map(RecordTransactionRequest.EntryRequest::accountId).distinct().count())
            throw new AccountRequestException("transaction.entry.invalid");

        var locked = accounts.findAndLockAllByIdOrdered(accountIds);

        if (locked.size() != accountIds.size())
            throw new AccountNotFoundException("account.not.found");

        Map<UUID, Account> byId = new HashMap<>();
        locked.forEach(a -> byId.put(a.getId(), a));
        String currency = locked.get(0).getCurrency();
        Currency c = Currency.find(currency).orElseThrow();
        long debit = 0, credit = 0;
        List<Long> amounts = new ArrayList<>();
        for (var e : request.entries()) {
            if (e == null || e.accountId() == null || e.direction() == null || e.amount() == null)
                throw new AccountRequestException("transaction.entry.invalid");

            Account a = byId.get(e.accountId());
            if (a == null) throw new AccountNotFoundException("account.not.found");

            if (!currency.equals(a.getCurrency())) throw new AccountRequestException("transaction.currency.mismatch");
            long minor;
            try {
                if (e.amount().signum() <= 0 || e.amount().scale() > c.minorUnitScale())
                    throw new ArithmeticException();
                minor = c.toMinorUnits(e.amount());
            } catch (ArithmeticException ex) {
                throw new AccountRequestException("transaction.amount.invalid", currency, c.minorUnitScale());
            }
            amounts.add(minor);
            if (e.direction() == backend.ledger_api.enums.EntryDirection.DEBIT) debit = Math.addExact(debit, minor);
            else credit = Math.addExact(credit, minor);
        }
        if (debit != credit)
            throw new AccountRequestException("transaction.not.balanced");

        String fingerprint = hash(request.description() + "|" + request.entries());
        var prior = idempotency.findByScopeAndIdempotencyKey(IDEMPOTENCY_SCOPE.name(), key);
        if (prior.isPresent()) {
            if (!prior.get().getRequestHash().equals(fingerprint))
                throw new AccountConflictException("idempotency.key.conflict");
            return read(prior.get().getResponseBody());
        }
        Map<UUID, Long> delta = new HashMap<>();
        for (int n = 0; n < request.entries().size(); n++) {
            var e = request.entries().get(n);
            long amount = amounts.get(n);
            delta.merge(e.accountId(), e.direction() == backend.ledger_api.enums.EntryDirection.CREDIT ? amount : -amount, Math::addExact);
        }
        for (Account a : locked) {
            long next = Math.addExact(a.getBalanceMinor(), delta.getOrDefault(a.getId(), 0L));
            if (next < -a.getOverdraftLimitMinor())
                throw new AccountConflictException("transaction.overdraft.exceeded", a.getId());
        }
        locked.forEach(a -> a.applyBalanceDeltaMinor(delta.getOrDefault(a.getId(), 0L)));
        Transaction tx = new Transaction(request.description(), null);
        for (int n = 0; n < request.entries().size(); n++) {
            var e = request.entries().get(n);
            tx.addEntry(new Entry(byId.get(e.accountId()), e.direction(), amounts.get(n)));
        }
        transactions.saveAndFlush(tx);
        TransactionResponse result = response(tx);
        try {
            idempotency.save(new IdempotencyRecord(IDEMPOTENCY_SCOPE.name(), key, fingerprint, tx.getId(), objectMapper.writeValueAsString(result)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID id) {
        return response(transactions.findById(id).orElseThrow(() -> new AccountNotFoundException("transaction.not.found", id)));
    }

    @Override
    @Transactional
    public TransactionResponse reverseTransaction(UUID transactionId, String idempotencyKey) {
        if (transactionId == null)
            throw new AccountRequestException("transaction.id.invalid");

        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 255)
            throw new AccountRequestException("idempotency.key.required");

        String requestFingerprint = hash("reverse|" + transactionId);
        IdempotencyScope reversalScope = IdempotencyScope.TRANSACTION_REVERSAL;
        var prior = idempotency.findByScopeAndIdempotencyKey(reversalScope.name(), idempotencyKey);
        if (prior.isPresent()) {
            if (!prior.get().getRequestHash().equals(requestFingerprint))
                throw new AccountConflictException("idempotency.key.conflict");
            return read(prior.get().getResponseBody());
        }

        Transaction original = transactions.findById(transactionId)
                .orElseThrow(() -> new AccountNotFoundException("transaction.not.found", transactionId));
        if (original.getReversalOf() != null)
            throw new AccountConflictException("transaction.reversal.not.allowed");

        if (transactions.findByReversalOfId(transactionId).isPresent())
            throw new AccountConflictException("transaction.already.reversed");

        List<UUID> accountIds = original.getEntries().stream().map(entry -> entry.getAccount().getId()).distinct().sorted().toList();

        List<Account> lockedAccounts = accounts.findAndLockAllByIdOrdered(accountIds);
        if (lockedAccounts.size() != accountIds.size())
            throw new AccountNotFoundException("account.not.found");
        if (transactions.findByReversalOfId(transactionId).isPresent())
            throw new AccountConflictException("transaction.already.reversed");

        Map<UUID, Account> accountsById = new HashMap<>();
        lockedAccounts.forEach(account -> accountsById.put(account.getId(), account));

        Map<UUID, Long> balanceDeltas = new HashMap<>();
        for (Entry originalEntry : original.getEntries()) {
            long reversedDelta = originalEntry.getDirection() == backend.ledger_api.enums.EntryDirection.DEBIT
                    ? originalEntry.getAmountMinor() : -originalEntry.getAmountMinor();
            balanceDeltas.merge(originalEntry.getAccount().getId(), reversedDelta, Math::addExact);
        }
        for (Account account : lockedAccounts) {
            long newBalance = Math.addExact(account.getBalanceMinor(), balanceDeltas.getOrDefault(account.getId(), 0L));
            if (newBalance < -account.getOverdraftLimitMinor())
                throw new AccountConflictException("transaction.overdraft.exceeded", account.getId());
        }
        lockedAccounts.forEach(account -> account.applyBalanceDeltaMinor(balanceDeltas.getOrDefault(account.getId(), 0L)));

        Transaction reversal = new Transaction("Reversal of " + original.getDescription(), original);
        for (Entry originalEntry : original.getEntries()) {
            var reversedDirection = originalEntry.getDirection() == backend.ledger_api.enums.EntryDirection.DEBIT
                    ? backend.ledger_api.enums.EntryDirection.CREDIT : backend.ledger_api.enums.EntryDirection.DEBIT;
            reversal.addEntry(new Entry(accountsById.get(originalEntry.getAccount().getId()), reversedDirection, originalEntry.getAmountMinor()));
        }
        transactions.saveAndFlush(reversal);
        TransactionResponse result = response(reversal);
        try {
            idempotency.save(new IdempotencyRecord(reversalScope.name(), idempotencyKey, requestFingerprint,
                    reversal.getId(), objectMapper.writeValueAsString(result)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return result;
    }

    private TransactionResponse response(Transaction t) {
        return new TransactionResponse(t.getId(), t.getDescription(), t.getRecordedAt(), t.getReversalOf() == null ? null : t.getReversalOf().getId(), t.getEntries().stream().map(e -> {
            Currency c = Currency.find(e.getAccount().getCurrency()).orElseThrow();
            return new TransactionResponse.EntryResponse(e.getAccount().getId(), e.getDirection(), c.fromMinorUnits(e.getAmountMinor()));
        }).toList());
    }

    private TransactionResponse read(String s) {
        try {
            return objectMapper.readValue(s, TransactionResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String hash(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
