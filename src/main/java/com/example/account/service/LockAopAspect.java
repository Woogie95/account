package com.example.account.service;

import com.example.account.aop.AccountLockIdInterface;
import com.example.account.dto.UseBalanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {

    private final LockService lockService;

    @Around("@annotation(com.example.account.aop.AccountLock) && args(request)")
    public Object aroundMethod(ProceedingJoinPoint proceedingJoinPoint, AccountLockIdInterface request) throws Throwable {
        // lock 취득 시도
        lockService.lock(request.getAccountNumber());
        try {
            return proceedingJoinPoint.proceed();
        } finally {
            // lock 해
            lockService.unlock(request.getAccountNumber());
        }

    }
}
