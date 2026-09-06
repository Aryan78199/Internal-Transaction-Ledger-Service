package com.example.ledger;

import com.example.ledger.entity.Wallet;
import com.example.ledger.repository.WalletRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class WalletDataInitializer implements ApplicationRunner {

    private final WalletRepository walletRepository;

    public WalletDataInitializer(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        walletRepository.findById(userId).ifPresentOrElse(
                wallet -> {},
                () -> walletRepository.save(new Wallet(userId, new BigDecimal("500.00")))
        );
    }
}
