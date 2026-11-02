package backend.ledger_api.controller;

import backend.ledger_api.dto.*;
import backend.ledger_api.service.TransactionService;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService ts) {
        transactionService = ts;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> record(@Valid @RequestBody RecordTransactionRequest request, @RequestHeader("Idempotency-Key") String key) {
        var r = transactionService.recordTransaction(request, key);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + r.id())).body(r);
    }

    @PostMapping("/{id}/reversal")
    public ResponseEntity<TransactionResponse> reverse(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key) {
        var reversal = transactionService.reverseTransaction(id, key);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + reversal.id())).body(reversal);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id) {
        return transactionService.getTransaction(id);
    }
}
