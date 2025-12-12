package com.cheque.chequerunner.util;

import com.cheque.chequerunner.domain.Account;
import com.cheque.chequerunner.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataLoader implements CommandLineRunner {

    private final AccountRepository accountRepository;

    public DataLoader(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Account drawer1 = new Account();
        drawer1.setBalance(new BigDecimal("5000.00"));
        drawer1.setAccountStatus(Account.AccountStatus.ACTIVE);
        accountRepository.save(drawer1);

        Account drawer2 = new Account();
        drawer2.setBalance(new BigDecimal("50.00"));
        drawer2.setAccountStatus(Account.AccountStatus.ACTIVE);
        accountRepository.save(drawer2);
    }
}