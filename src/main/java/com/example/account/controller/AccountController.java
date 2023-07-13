package com.example.account.controller;

import com.example.account.dto.CreateAccountDTO;
import com.example.account.dto.DeleteAccountDTO;
import com.example.account.entity.Account;
import com.example.account.dto.AccountInfo;
import com.example.account.service.AccountService;
import com.example.account.service.RedisTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final RedisTestService redisTestService;

    @PostMapping("/account")
    public CreateAccountDTO.Response createAccount(
            @RequestBody @Valid CreateAccountDTO.Request createRequest) {

        return CreateAccountDTO.form(accountService.createAccount(
                createRequest.getUserId(), createRequest.getInitialBalance()));
    }

    @DeleteMapping("/account")
    public DeleteAccountDTO.Response deleteAccount(
            @RequestBody @Valid DeleteAccountDTO.Request deleteRequest) {

        return DeleteAccountDTO.form(accountService.deleteAccount(
                deleteRequest.getUserId(), deleteRequest.getAccountNumber()));
    }

    @GetMapping("/account")
    public List<AccountInfo> getAccountsByUserId(@RequestParam("user_id") Long userId) {
        return accountService.getAccountsByUserId(userId).stream().map(AccountDTO -> AccountInfo.builder()
                .accountNumber(AccountDTO.getAccountNumber())
                .balance(AccountDTO.getBalance())
                .build()).collect(Collectors.toList());
    }

    @GetMapping("/get-lock")
    public String getLock() {
        return redisTestService.getLock();
    }


    @GetMapping("/account/{id}")
    public Optional<Account> getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }
}
