package com.example.account.service;

import com.example.account.entity.Account;
import com.example.account.entity.AccountStatus;
import com.example.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    @Transactional
    public void createAccount() {
        Account account = Account.builder()
                .accountNumber("40000")
                .accountStatus(AccountStatus.IN_USE)
                .build();
        accountRepository.save(account);
    }

    @Transactional
    public Optional<Account> getAccount(Long id) {
        if (id < 0){
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id);
    }
}
