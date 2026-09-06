package com.example.ledger.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        String type,
        BigDecimal remainingBalance,
        String message
) {
}
