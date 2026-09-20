import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import {
  Account,
  AccountType,
  Asset,
  CasaEnrollment,
  CasaPackage,
  CitadInquiry,
  CreateAssetRequest,
  CreditRating,
  CustomerProfile,
  InterbankTransferResponse,
  LoginResponse,
  Transaction,
  TransferResult,
  User,
} from "./api.models";
import { createIdempotencyKey } from "./core/http/idempotency-key";

@Injectable({ providedIn: "root" })
export class ApiService {
  private readonly http = inject(HttpClient);

  login(email: string, password: string) {
    return this.http.post<LoginResponse>("/api/user/login", {
      email,
      password,
    });
  }

  register(fullName: string, email: string, password: string) {
    return this.http.post<User>("/api/user/register", {
      fullName,
      email,
      password,
    });
  }

  profile() {
    return this.http.get<User>("/api/user/me");
  }

  updateProfile(fullName: string, email: string) {
    return this.http.put<User>("/api/user/me", { fullName, email });
  }

  changePassword(currentPass: string, newPass: string) {
    return this.http.put<void>("/api/user/password", { currentPass, newPass });
  }

  accounts() {
    return this.http.get<Account[]>("/api/account");
  }

  createAccount(accountType: AccountType) {
    return this.http.post<Account>("/api/account/create", { accountType });
  }

  transactions(accountId?: number) {
    const endpoint = accountId
      ? `/api/transaction/account/${accountId}`
      : "/api/transaction";
    return this.http.get<Transaction[]>(endpoint);
  }

  money(accountId: number, action: "deposit" | "withdraw", amount: number) {
    return this.http.post<Account>(`/api/account/${accountId}/${action}`, {
      amount,
    });
  }

  transfer(
    senderAccountNumber: string,
    receiverAccountNumber: string,
    amount: number,
  ) {
    return this.http.post<TransferResult>(
      "/api/transfers/internal",
      {
        fromAccount: senderAccountNumber,
        toAccount: receiverAccountNumber,
        amount,
        currency: "VND",
        description: "Chuyển tiền từ ứng dụng web",
      },
      { headers: { "Idempotency-Key": createIdempotencyKey() } },
    );
  }

  interbankTransfer(
    senderAccountNumber: string,
    bankCode: string,
    receiverAccountNumber: string,
    recipientName: string,
    amount: number,
  ) {
    return this.http.post<InterbankTransferResponse>(
      "/api/account/transfer/interbank",
      {
        senderAccountNumber,
        bankCode,
        receiverAccountNumber,
        recipientName,
        amount,
      },
    );
  }

  customerProfile() {
    return this.http.get<CustomerProfile>("/api/customer/me");
  }

  updateCustomerProfile(
    phoneNumber: string,
    address: string,
    dateOfBirth: string,
  ) {
    return this.http.put<CustomerProfile>("/api/customer/me", {
      phoneNumber: phoneNumber || null,
      address: address || null,
      dateOfBirth: dateOfBirth || null,
    });
  }

  casa() {
    return this.http.get<CasaEnrollment>("/api/casa/me");
  }

  enrollCasa(settlementAccountNumber: string, packageType: CasaPackage) {
    return this.http.post<CasaEnrollment>("/api/casa/enroll", {
      settlementAccountNumber,
      packageType,
    });
  }

  creditRating() {
    return this.http.get<CreditRating>("/api/credit-rating/me");
  }

  evaluateCredit() {
    return this.http.post<CreditRating>("/api/credit-rating/evaluate", {});
  }

  assets() {
    return this.http.get<Asset[]>("/api/assets");
  }

  createAsset(asset: CreateAssetRequest) {
    return this.http.post<Asset>("/api/assets", asset);
  }

  deleteAsset(id: number) {
    return this.http.delete<void>(`/api/assets/${id}`);
  }

  citadInquiries() {
    return this.http.get<CitadInquiry[]>("/api/citad/inquiries");
  }

  createCitadInquiry(transactionId: number, reason: string) {
    return this.http.post<CitadInquiry>("/api/citad/inquiries", {
      transactionId,
      reason,
    });
  }
}
