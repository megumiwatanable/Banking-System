export type AccountType = "SAVINGS" | "CURRENT";
export type AccountStatus = "ACTIVE" | "FROZEN" | "BLOCKED" | "CLOSED";
export type TransactionType =
  | "DEPOSIT"
  | "WITHDRAW"
  | "TRANSFER"
  | "INTERNAL_TRANSFER"
  | "INTERBANK_TRANSFER"
  | "BILL_PAYMENT"
  | "CARD_PAYMENT"
  | "LOAN_DISBURSEMENT"
  | "LOAN_REPAYMENT"
  | "FEE"
  | "REVERSAL";
export type TransferMode = "internal" | "interbank";
export type CasaPackage = "BASIC" | "PREMIUM";

export interface User {
  id: number;
  fullName: string;
  email: string;
}

export interface LoginResponse extends User {
  userId: number;
  token: string;
  tokenType: string;
}

export interface Account {
  id: number;
  accountNumber: string;
  accountType: AccountType;
  balance: number;
  availableBalance: number;
  holdAmount: number;
  currency: string;
  status: AccountStatus;
}

export interface Transaction {
  id: number;
  transactionType: TransactionType;
  amount: number;
  senderAccountNumber: string | null;
  receiverAccountNumber: string | null;
  transactionTime: string;
  status: string;
}

export interface InterbankTransferResponse {
  transactionId: number;
  status: "PENDING";
  amount: number;
}

export interface TransferResult {
  transactionId: number;
  status: string;
  amount: number;
  fee: number;
  currency: string;
  transactionTime: string;
}

export interface ApiError {
  message?: string;
  validationErrors?: string[];
}

export interface CustomerProfile {
  id: number;
  customerNumber: string;
  fullName: string;
  email: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  kycStatus: string;
}

export interface CasaEnrollment {
  id: number;
  settlementAccountNumber: string;
  packageType: CasaPackage;
  status: string;
  enrolledAt: string;
}

export interface CreditRating {
  score: number;
  grade: string;
  explanation: string;
  evaluatedAt: string;
  disclaimer: string;
}

export interface Asset {
  id: number;
  assetType: string;
  assetName: string;
  estimatedValue: number;
  currency: string;
  description?: string;
  updatedAt: string;
}

export type CreateAssetRequest = Omit<Asset, "id" | "updatedAt">;

export interface CitadInquiry {
  id: number;
  referenceNumber: string;
  transactionId: number;
  status: string;
  reason: string;
  bankResponse?: string;
  createdAt: string;
  processingMode: string;
}
