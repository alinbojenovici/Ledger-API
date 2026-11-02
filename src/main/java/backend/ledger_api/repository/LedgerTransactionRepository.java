package backend.ledger_api.repository;

import backend.ledger_api.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface LedgerTransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByReversalOfId(UUID originalTransactionId);
}
