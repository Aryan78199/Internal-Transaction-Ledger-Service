package com.example.ledger;

import com.example.ledger.dto.TransactionRequest;
import com.example.ledger.dto.TransactionResponse;
import com.example.ledger.entity.Wallet;
import com.example.ledger.repository.TransactionRepository;
import com.example.ledger.repository.WalletRepository;
import com.example.ledger.service.TransactionService;
import com.example.ledger.service.TransactionService.InsufficientFundsException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Processes a single valid debit transaction successfully.")
    void processesSingleValidDebitSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        walletRepository.save(new Wallet(userId, new BigDecimal("500.00")));

        TransactionRequest request = new TransactionRequest(
                transactionId,
                userId,
                new BigDecimal("100.00"),
                "DEBIT"
        );

        TransactionResponse response = transactionService.process(request);
        Wallet wallet = walletRepository.findById(userId).orElseThrow();

        assertEquals(new BigDecimal("400.00"), wallet.getBalance());
        assertEquals(new BigDecimal("400.00"), response.remainingBalance());
        assertEquals(1, transactionRepository.count());
    }

    @Test
    @Order(2)
    @DisplayName("Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.")
    void handlesThreeIdenticalTransactionsConcurrently() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        walletRepository.save(new Wallet(userId, new BigDecimal("500.00")));

        TransactionRequest request = new TransactionRequest(
                transactionId,
                userId,
                new BigDecimal("100.00"),
                "DEBIT"
        );

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TransactionResponse>> futures = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return transactionService.process(request);
            }));
        }

        start.countDown();

        List<TransactionResponse> responses = new ArrayList<>();
        for (Future<TransactionResponse> future : futures) {
            responses.add(future.get(5, TimeUnit.SECONDS));
        }

        executor.shutdown();

        Wallet wallet = walletRepository.findById(userId).orElseThrow();
        assertEquals(new BigDecimal("400.00"), wallet.getBalance());
        assertEquals(1, transactionRepository.count());
        assertEquals(3, responses.size());
    }

    @Test
    @Order(3)
    @DisplayName("Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.")
    void preventsNegativeBalanceDuringConcurrentDebits() throws Exception {
        UUID userId = UUID.randomUUID();

        walletRepository.save(new Wallet(userId, new BigDecimal("500.00")));

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            final UUID transactionId = UUID.randomUUID();
            futures.add(executor.submit(() -> {
                start.await();
                try {
                    TransactionRequest request = new TransactionRequest(
                            transactionId,
                            userId,
                            new BigDecimal("100.00"),
                            "DEBIT"
                    );
                    transactionService.process(request);
                    return true;
                } catch (InsufficientFundsException e) {
                    return false;
                }
            }));
        }

        start.countDown();

        int successfulRequests = 0;
        int failedRequests = 0;

        for (Future<Boolean> future : futures) {
            boolean successful = future.get(5, TimeUnit.SECONDS);
            if (successful) {
                successfulRequests++;
            } else {
                failedRequests++;
            }
        }

        executor.shutdown();

        Wallet wallet = walletRepository.findById(userId).orElseThrow();
        assertEquals(5, successfulRequests);
        assertEquals(5, failedRequests);
        assertEquals(new BigDecimal("0.00"), wallet.getBalance());
        assertEquals(5, transactionRepository.count());
    }
}
