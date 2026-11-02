package backend.ledger_api.entity;

import backend.ledger_api.enums.EntryDirection;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "LA_ENTRY")
public class Entry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TRANSACTION_ID", nullable = false, updatable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ACCOUNT_ID", nullable = false, updatable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "DIRECTION", nullable = false, length = 6, updatable = false)
    private EntryDirection direction;

    @Column(name = "AMOUNT_MINOR", nullable = false, updatable = false)
    private long amountMinor;

    protected Entry() {
    }

    public Entry(Account account, EntryDirection direction, long amountMinor) {
        this.account = account;
        this.direction = direction;
        this.amountMinor = amountMinor;
    }

    void attachTo(Transaction transaction) {
        this.transaction = transaction;
    }

    public UUID getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public EntryDirection getDirection() {
        return direction;
    }

    public long getAmountMinor() {
        return amountMinor;
    }
}
