package com.example.account.service;

import com.example.account.dto.AccountDTO;
import com.example.account.entity.Account;
import com.example.account.entity.AccountUser;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.example.account.type.ErrorCode.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        // given
        AccountUser user = AccountUser.builder()
                .userName("뽀로로").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012").build()));

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        // when
        AccountDTO accountDTO = accountService.createAccount(1L, 100L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDTO.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 해제 테스트 코드")
    void deleteAccountSuccess() {
        // given
        AccountUser user = AccountUser.builder()
                .userName("뽀로로").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        // when
        AccountDTO accountDTO = accountService.deleteAccount(1L, "1234567890");

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDTO.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UN_USE, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("값이 비어 있을 때 초기값")
    void createFirstSuccess() {
        // given
        AccountUser user = AccountUser.builder()
                .userName("뽀로로").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        // when
        AccountDTO accountDTO = accountService.createAccount(1L, 100L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDTO.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_userNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        // then
        assertEquals(USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해제 실패")
    void deleteAccount_userNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccount_AccountNotFound() {
        // given
        AccountUser user = AccountUser.builder()
                .userName("뽀로로").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌 잔액 없음 테스트")
    void deleteAccountFailed_balanceNotFound() {
        // given
        AccountUser user = AccountUser.builder()
                .userName("뽀로로").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(1000L)
                        .accountNumber("1234567890")
                        .build()));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해지한 계좌 해지 불가 테스트")
    void deleteAccountFailed_already_unRegistered() {
        // given
        AccountUser user = AccountUser.builder()
                .userName("뽀로로").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountStatus(AccountStatus.UN_USE)
                        .balance(0L)
                        .accountNumber("1234567890")
                        .build()));

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }


    @Test
    @DisplayName("유저 당 최대 계좌는 10개")
    void createAccount_maxAccountIs10() {
        // given
        AccountUser user = AccountUser.builder()
                .userName("뽀로로")
                .build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        // then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 조회 성공")
    void AccountInquirySuccessful() {
        // given
        given(accountRepository.findById(anyLong()))
                .willReturn(Optional.of(Account.builder()
                        .accountStatus(AccountStatus.UN_USE)
                        .accountNumber("65789").build()));

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);

        // when
        Optional<Account> account = accountService.getAccount(4555L);

        // then
        verify(accountRepository, times(1)).findById(captor.capture());
        verify(accountRepository, times(0)).save(any());

        assertEquals(4555L, captor.getValue()); // 4555L 이 getValue 여야만 한다.
        assertEquals("65789", account.get().getAccountNumber());
        assertEquals(AccountStatus.UN_USE, account.get().getAccountStatus());
    }

    @Test
    @DisplayName("계좌 조회 실패 - 음수로 조회")
    void testFailedToSearchAccount() {
        // given
        // when
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.getAccount(-10L));

        // then
        assertEquals("Minus", exception.getMessage());
    }

    @Test
    @DisplayName("해당 유저의 모든 계좌 가져오기")
    void successGetAccountsByUserId() {
        // given
        AccountUser user = AccountUser.builder()
                .userName("뽀로로")
                .build();
        user.setId(15L);

        List<Account> accountList = Arrays.asList(
                Account.builder()
                        .accountUser(user)
                        .accountNumber("1234567890")
                        .balance(10000L)
                        .build(),
                Account.builder()
                        .accountUser(user)
                        .accountNumber("1111111111")
                        .balance(11111L)
                        .build(),
                Account.builder()
                        .accountUser(user)
                        .accountNumber("2222222222")
                        .balance(22222L)
                        .build());

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accountList);
        // when
        List<AccountDTO> accountDTOList = accountService.getAccountsByUserId(1L);

        // then
        assertEquals(3, accountDTOList.size());
        assertEquals("1234567890", accountDTOList.get(0).getAccountNumber());
        assertEquals(10000L, accountDTOList.get(0).getBalance());
        assertEquals("1111111111", accountDTOList.get(1).getAccountNumber());
        assertEquals(11111L, accountDTOList.get(1).getBalance());
        assertEquals("2222222222", accountDTOList.get(2).getAccountNumber());
        assertEquals(22222L, accountDTOList.get(2).getBalance());
    }

    @Test
    @DisplayName("해당 유저의 모든 계좌 가져오기 실패")
    void failedToGetAccounts() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        // then
        assertEquals(USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름")
    void deleteAccountFailed_userUnMatch() {
        // given
        AccountUser user = AccountUser.builder()
                .userName("루피")
                .build();
        user.setId(10L);
        AccountUser accountUser = AccountUser.builder()
                .userName("뽀로로")
                .build();
        user.setId(11L);
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
                , () -> accountService.deleteAccount(1L, "1000000003"));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }
}