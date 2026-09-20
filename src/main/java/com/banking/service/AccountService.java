package com.banking.service;

import com.banking.dto.account.*;
import jakarta.validation.Valid;
import java.util.List;

public interface AccountService {
    List<AccountResponse> getMyAccounts();
    AccountResponse create(@Valid CreateAccountRequest request);

    AccountResponse getAccountById(Long accountId);

    AccountResponse deposit(Long accountId, @Valid DepositRequest request);

    AccountResponse withdraw(Long accountId, @Valid WithdrawRequest request);

    AccountResponse transfer(@Valid TransferRequest request);

    InterbankTransferResponse requestInterbankTransfer(@Valid InterbankTransferRequest request);
}
