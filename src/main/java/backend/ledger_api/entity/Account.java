package backend.ledger_api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="LA_ACCOUNT")
public class Account {
 @Id @GeneratedValue(strategy=GenerationType.UUID) @Column(name="ID",nullable=false,updatable=false) private UUID id;
 @Column(name="CURRENCY",nullable=false,length=3) private String currency;
 @Column(name="OVERDRAFT_LIMIT_MINOR",nullable=false) private long overdraftLimitMinor;
 @Column(name="BALANCE_MINOR",nullable=false) private long balanceMinor;
 @Column(name="CREATED_AT",nullable=false,updatable=false) private Instant createdAt;
 protected Account() {}
 public Account(String currency,long overdraftLimitMinor){this.currency=currency;this.overdraftLimitMinor=overdraftLimitMinor;this.balanceMinor=0;this.createdAt=Instant.now();}
 public void applyBalanceDeltaMinor(long delta){this.balanceMinor=Math.addExact(this.balanceMinor,delta);}
 public UUID getId(){return id;} public String getCurrency(){return currency;} public long getOverdraftLimitMinor(){return overdraftLimitMinor;} public long getBalanceMinor(){return balanceMinor;} public Instant getCreatedAt(){return createdAt;}
}
