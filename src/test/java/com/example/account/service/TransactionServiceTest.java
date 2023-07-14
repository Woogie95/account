package com.example.account.service;

import com.example.account.dto.TransactionDTO;
import com.example.account.entity.Account;
import com.example.account.entity.AccountUser;
import com.example.account.entity.Transaction;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UN_USE;
import static com.example.account.type.ErrorCode.USER_NOT_FOUND;
import static com.example.account.type.TransactionResultType.FAILED;
import static com.example.account.type.TransactionResultType.SUCCESS;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 성공")
    void successUseBalance() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .userName("루피")
                .build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000002")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(10000L)
                        .accountStatus(IN_USE)
                        .accountNumber("1000000012").build()));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(SUCCESS)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDTO transactionDTO = transactionService.useBalance(1L,
                "1000000000", 1000L);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(9000L, captor.getValue().getBalanceSnapshot());
        assertEquals(SUCCESS, transactionDTO.getTransactionResultType());
        assertEquals(USE, transactionDTO.getTransactionType());
        assertEquals(9000L, transactionDTO.getBalanceSnapshot());
        assertEquals(1000L, transactionDTO.getAmount());
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_userNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 10000L));

        // then
        assertEquals(USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void useBalance_AccountNotFound() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .userName("뽀로로").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 10000L));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 살패")
    void useBalanceFailed_userUnMatch() {
        // given
        AccountUser user = AccountUser.builder()
                .id(10L)
                .userName("루피")
                .build();
        AccountUser accountUser = AccountUser.builder()
                .id(11L)
                .userName("뽀로로")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .balance(0L)
                        .accountNumber("1000000001")
                        .build()));
        // when
        AccountException exception = assertThrows(AccountException.class
                , () -> transactionService.useBalance(1L, "1000000003", 10000L));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 사용할 수 없음")
    void useBalanceFailed_alreadyUnregistered() {
        // given
        AccountUser user = AccountUser.builder()
                .id(2L)
                .userName("루피")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountStatus(UN_USE)
                        .balance(0L)
                        .accountNumber("1111111111")
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class
                , () -> transactionService.useBalance(1L, "1234567890", 10000L));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액 보다 큰  경우")
    void exceedAmount_UseBalance() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .userName("루피")
                .build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1000000002")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class
                , () -> transactionService.useBalance(1L, "1234567890", 1000L));

        // then
        verify(transactionRepository, times(0)).save(any());
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUserTransaction() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .userName("루피")
                .build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000002")
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(10000L)
                        .accountStatus(IN_USE)
                        .accountNumber("1000000012").build()));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(SUCCESS)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        transactionService.saveFailedUseTransaction("1000000000", 1000L);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(FAILED, captor.getValue().getTransactionResultType());
    }


    @Test
    @DisplayName("잔액 사용 성공")
    void successCancelBalance() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .userName("루피")
                .build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000002")
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResultType(SUCCESS)
                        .transactionId("transactionId123")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(10000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDTO transactionDTO = transactionService.cancelBalance("transactionId123",
                "1000000000", 1000L);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(10000L + 1000L, captor.getValue().getBalanceSnapshot());
        assertEquals(SUCCESS, transactionDTO.getTransactionResultType());
        assertEquals(CANCEL, transactionDTO.getTransactionType());
        assertEquals(10000L, transactionDTO.getBalanceSnapshot());
        assertEquals(1000L, transactionDTO.getAmount());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 사용 취소 실패")
    void cancelTransaction_AccountNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 10000L));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("원 사용 거래 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 10000L));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌 매칭 실패 - 계좌 사용 취소 실패")
    void cancelTransaction_TransactionAccountUntMatch() {
        // given
        AccountUser user = AccountUser.builder()
                .id(10L)
                .userName("루피")
                .build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000001")
                .build();
        Account accountNotUse = Account.builder()
                .id(2L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000011")
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 1000L));

        // then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래금액과 취소 금액이 다름 - 계좌 사용 취소 실패")
    void cancelTransaction_TooOldOrderToCancel() {
        // given
        AccountUser user = AccountUser.builder()
                .id(10L)
                .userName("루피")
                .build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000001")
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L + 1000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 1000L));

        // then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("취소는 1년까지만 가능하다. - 계좌 사용 취소 실패")
    void cancelTransaction_CancelMustFully() {
        // given
        AccountUser user = AccountUser.builder()
                .id(10L)
                .userName("루피")
                .build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000001")
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567890", 1000L));

        // then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, accountException.getErrorCode());
    }

    @Test
    void successQueryTransaction() {
        // given
        AccountUser user = AccountUser.builder()
                .id(10L)
                .userName("루피")
                .build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000001")
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        // when
        TransactionDTO transactionDTO = transactionService.queryTransaction("trxId");

        // then
        assertEquals(USE, transactionDTO.getTransactionType());
        assertEquals(SUCCESS, transactionDTO.getTransactionResultType());
        assertEquals(1000L, transactionDTO.getAmount());
        assertEquals("transactionId", transactionDTO.getTransactionId());

    }

    @Test
    @DisplayName("원 거래 없음 - 잔액 사용 취소 실패")
    void queryTransaction_TransactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }
}