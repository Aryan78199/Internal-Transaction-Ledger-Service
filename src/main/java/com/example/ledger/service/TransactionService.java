package com.example.ledger.service;

import com.example.ledger.dto.TransactionRequest;
import com.example.ledger.dto.TransactionResponse;
import com.example.ledger.entity.Transaction;
import com.example.ledger.entity.Wallet;
import com.example.ledger.repository.TransactionRepository;
import com.example.ledger.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse process(TransactionRequest request) {

        Wallet wallet = walletRepository
                .findByUserIdForUpdate(request.userId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Wallet not found"));

        var existing = transactionRepository
                .findByTransactionId(request.transactionId());

        if (existing.isPresent()) {
            Transaction transaction = existing.get();
            return new TransactionResponse(
                    transaction.getTransactionId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getType(),
                    wallet.getBalance(),
                    "Duplicate request. Returning cached result."
            );
        }

        if (!"DEBIT".equalsIgnoreCase(request.type())) {
            throw new IllegalArgumentException(
                    "Only DEBIT transactions are supported"
            );
        }

        if (request.amount() == null ||
                request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }

        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds"
            );
        }

        BigDecimal newBalance =
                wallet.getBalance().subtract(request.amount());

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction(
                request.transactionId(),
                request.userId(),
                request.amount(),
                request.type()
        );

        transactionRepository.save(transaction);

        return new TransactionResponse(
                request.transactionId(),
                request.userId(),
                request.amount(),
                request.type(),
                newBalance,
                "Transaction processed successfully."
        );
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }
}
