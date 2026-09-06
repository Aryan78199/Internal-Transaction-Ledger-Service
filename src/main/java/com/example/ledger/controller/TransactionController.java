package com.example.ledger.controller;

import com.example.ledger.dto.TransactionRequest;
import com.example.ledger.dto.TransactionResponse;
import com.example.ledger.service.TransactionService;
import com.example.ledger.service.TransactionService.InsufficientFundsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> process(
            @RequestBody TransactionRequest request
    ) {
        try {
            TransactionResponse response = transactionService.process(request);
            return ResponseEntity.ok(response);
        } catch (InsufficientFundsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
