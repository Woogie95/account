package com.example.account.controller;

import com.example.account.aop.AccountLock;
import com.example.account.dto.CancelBalanceDTO;
import com.example.account.dto.QueryTransactionResponse;
import com.example.account.dto.UseBalanceDTO;
import com.example.account.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalanceDTO.Response useBalance(@Valid @RequestBody UseBalanceDTO.Request request) throws InterruptedException {
        try {
            Thread.sleep(5000L);
            return UseBalanceDTO.Response.form(transactionService.useBalance(
                    request.getUserId(), request.getAccountNumber(), request.getAmount()));
        } catch (ArithmeticException e) {
            log.error("Failed to use balance");
            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e;
        }
    }

    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalanceDTO.Response useBalance(@Valid @RequestBody CancelBalanceDTO.Request request) {
        try {
            return CancelBalanceDTO.Response.form(transactionService.cancelBalance(
                    request.getTransactionId(), request.getAccountNumber(), request.getAmount()));
        } catch (ArithmeticException e) {
            log.error("Failed to use balance");
            transactionService.saveFailedCancelTransaction(request.getAccountNumber(), request.getAmount());
            throw e;
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(@PathVariable String transactionId) {
        return QueryTransactionResponse.form(transactionService.queryTransaction(transactionId));
    }

}
