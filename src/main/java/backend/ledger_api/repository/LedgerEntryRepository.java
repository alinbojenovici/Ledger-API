package backend.ledger_api.repository;

import backend.ledger_api.entity.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<Entry, UUID> {}
