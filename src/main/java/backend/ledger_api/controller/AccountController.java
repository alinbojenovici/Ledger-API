package backend.ledger_api.controller;

import backend.ledger_api.dto.AccountResponse;
import backend.ledger_api.dto.CreateAccountRequest;
import backend.ledger_api.service.AccountService;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody CreateAccountRequest request, @RequestHeader("Idempotency-Key") String key) {
        AccountResponse response = accounts.openAccount(request, key);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.id())).body(response);
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable UUID accountId) {
        return accounts.getAccount(accountId);
    }
}
