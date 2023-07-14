package com.example.account.controller;

import com.example.account.dto.AccountDTO;
import com.example.account.dto.CancelBalanceDTO;
import com.example.account.dto.TransactionDTO;
import com.example.account.dto.UseBalanceDTO;
import com.example.account.service.TransactionService;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.example.account.type.TransactionResultType.SUCCESS;
import static com.example.account.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successUseBalance() throws Exception {
        // given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willReturn(TransactionDTO.builder()
                        .accountNumber("1234567890")
                        .transactedAt(LocalDateTime.of(2023, 7, 13, 10, 15, 10))
                        .amount(10000L)
                        .transactionId("transactionId")
                        .transactionResultType(SUCCESS)
                        .build());
        // when
        // then
        mockMvc.perform(post("/transaction/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UseBalanceDTO.Request(1L, "1111111111", 3000L)
                        ))
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactedAt").value(
                        LocalDateTime.of(2023, 7, 13, 10, 15, 10).toString()))
                .andExpect(jsonPath("$.amount").value(10000L))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.transactionResultType").value(SUCCESS.toString()));
    }

    @Test
    void successCancelBalance() throws Exception {
        // given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
                .willReturn(TransactionDTO.builder()
                        .accountNumber("1234567890")
                        .transactedAt(LocalDateTime.of(2023, 7, 13, 10, 15, 10))
                        .amount(10000L)
                        .transactionId("transactionId")
                        .transactionResultType(SUCCESS)
                        .build());
        // when
        // then
        mockMvc.perform(post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelBalanceDTO.Request("transactionId", "1111111111", 3000L)
                        ))
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactedAt").value(
                        LocalDateTime.of(2023, 7, 13, 10, 15, 10).toString()))
                .andExpect(jsonPath("$.amount").value(10000L))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.transactionResultType").value(SUCCESS.toString()));
    }

    @Test
    @DisplayName("")
    void successGetQueryTransaction() throws Exception {
        // given
        given(transactionService.queryTransaction(anyString()))
                .willReturn(TransactionDTO.builder()
                        .accountNumber("1234567890")
                        .transactionType(USE)
                        .transactedAt(LocalDateTime.of(2023, 7, 13, 10, 15, 10))
                        .amount(10000L)
                        .transactionId("transactionId")
                        .transactionResultType(SUCCESS)
                        .build());

        // when
        // then
        mockMvc.perform(get("/transaction/12345"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactionType").value("USE"))
                .andExpect(jsonPath("$.transactedAt").value(
                        LocalDateTime.of(2023, 7, 13, 10, 15, 10).toString()))
                .andExpect(jsonPath("$.amount").value(10000L))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.transactionResultType").value("SUCCESS"));
    }

}