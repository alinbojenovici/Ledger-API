package backend.ledger_api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "LA_TRANSACTION")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "DESCRIPTION", nullable = false, length = 500)
    private String description;

    @Column(name = "RECORDED_AT", nullable = false, updatable = false)
    private Instant recordedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REVERSAL_OF_ID", unique = true)
    private Transaction reversalOf;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    private List<Entry> entries = new ArrayList<>();

    protected Transaction() {
    }

    public Transaction(String description, Transaction reversalOf) {
        this.description = description;
        this.reversalOf = reversalOf;
        this.recordedAt = Instant.now();
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
        entry.attachTo(this);
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public Transaction getReversalOf() {
        return reversalOf;
    }

    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }
}
