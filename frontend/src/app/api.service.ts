import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface User { id: number; fullName: string; email: string }
export interface LoginResponse extends User { userId: number; token: string; tokenType: string }
export interface Account { id: number; accountNumber: string; accountType: 'SAVINGS' | 'CURRENT'; balance: number }
export interface Transaction { id: number; transactionType: 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER' | 'INTERBANK_TRANSFER'; amount: number; senderAccountNumber: string | null; receiverAccountNumber: string | null; transactionTime: string; status: string }
export interface InterbankTransferResponse { transactionId: number; status: 'PENDING'; amount: number }
export interface ApiError { message?: string; validationErrors?: string[] }

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  login(email: string, password: string) { return this.http.post<LoginResponse>('/api/user/login', { email, password }); }
  register(fullName: string, email: string, password: string) { return this.http.post<User>('/api/user/register', { fullName, email, password }); }
  profile() { return this.http.get<User>('/api/user/me'); }
  updateProfile(fullName: string, email: string) { return this.http.put<User>('/api/user/me', { fullName, email }); }
  changePassword(currentPass: string, newPass: string) { return this.http.put<void>('/api/user/password', { currentPass, newPass }); }
  accounts() { return this.http.get<Account[]>('/api/account'); }
  createAccount(accountType: Account['accountType']) { return this.http.post<Account>('/api/account/create', { accountType }); }
  transactions(accountId?: number) { return this.http.get<Transaction[]>(accountId ? `/api/transaction/account/${accountId}` : '/api/transaction'); }
  money(accountId: number, action: 'deposit' | 'withdraw', amount: number) {
    return this.http.post<Account>(`/api/account/${accountId}/${action}`, { amount });
  }
  transfer(senderAccountNumber: string, receiverAccountNumber: string, amount: number) {
    return this.http.post<Account>('/api/account/transfer', { senderAccountNumber, receiverAccountNumber, amount });
  }
  interbankTransfer(senderAccountNumber: string, bankCode: string, receiverAccountNumber: string, recipientName: string, amount: number) {
    return this.http.post<InterbankTransferResponse>('/api/account/transfer/interbank', { senderAccountNumber, bankCode, receiverAccountNumber, recipientName, amount });
  }
}
