package com.example.account.dto;

import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryTransactionResponse {
    private String accountNumber;
    private TransactionType transactionType;
    private TransactionResultType transactionResultType;
    private String transactionId;
    private Long amount;
    private LocalDateTime transactedAt;

    public static QueryTransactionResponse form(TransactionDTO transactionDTO) {
        return QueryTransactionResponse.builder()
                .accountNumber(transactionDTO.getAccountNumber())
                .transactionType(transactionDTO.getTransactionType())
                .transactionResultType(transactionDTO.getTransactionResultType())
                .transactionId(transactionDTO.getTransactionId())
                .amount(transactionDTO.getAmount())
                .transactedAt(transactionDTO.getTransactedAt())
                .build();
    }
}
