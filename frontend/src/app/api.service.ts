import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface User { id: number; fullName: string; email: string }
export interface LoginResponse extends User { userId: number; token: string; tokenType: string }
export interface Account { id: number; accountNumber: string; accountType: 'SAVINGS' | 'CURRENT'; balance: number }
export interface Transaction { id: number; transactionType: 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER' | 'INTERBANK_TRANSFER'; amount: number; senderAccountNumber: string | null; receiverAccountNumber: string | null; transactionTime: string; status: string }
export interface InterbankTransferResponse { transactionId: number; status: 'PENDING'; amount: number }
export interface ApiError { message?: string; validationErrors?: string[] }
export interface CustomerProfile { id: number; customerNumber: string; fullName: string; email: string; phoneNumber?: string; address?: string; dateOfBirth?: string; kycStatus: string }
export interface CasaEnrollment { id: number; settlementAccountNumber: string; packageType: 'BASIC' | 'PREMIUM'; status: string; enrolledAt: string }
export interface CreditRating { score: number; grade: string; explanation: string; evaluatedAt: string; disclaimer: string }
export interface Asset { id: number; assetType: string; assetName: string; estimatedValue: number; currency: string; description?: string; updatedAt: string }
export interface CitadInquiry { id: number; referenceNumber: string; transactionId: number; status: string; reason: string; bankResponse?: string; createdAt: string; processingMode: string }

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
  customerProfile() { return this.http.get<CustomerProfile>('/api/customer/me'); }
  updateCustomerProfile(phoneNumber: string, address: string, dateOfBirth: string) { return this.http.put<CustomerProfile>('/api/customer/me', { phoneNumber: phoneNumber || null, address: address || null, dateOfBirth: dateOfBirth || null }); }
  casa() { return this.http.get<CasaEnrollment>('/api/casa/me'); }
  enrollCasa(settlementAccountNumber: string, packageType: 'BASIC' | 'PREMIUM') { return this.http.post<CasaEnrollment>('/api/casa/enroll', { settlementAccountNumber, packageType }); }
  creditRating() { return this.http.get<CreditRating>('/api/credit-rating/me'); }
  evaluateCredit() { return this.http.post<CreditRating>('/api/credit-rating/evaluate', {}); }
  assets() { return this.http.get<Asset[]>('/api/assets'); }
  createAsset(asset: Omit<Asset, 'id' | 'updatedAt'>) { return this.http.post<Asset>('/api/assets', asset); }
  deleteAsset(id: number) { return this.http.delete<void>(`/api/assets/${id}`); }
  citadInquiries() { return this.http.get<CitadInquiry[]>('/api/citad/inquiries'); }
  createCitadInquiry(transactionId: number, reason: string) { return this.http.post<CitadInquiry>('/api/citad/inquiries', { transactionId, reason }); }
}
