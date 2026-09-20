export interface Beneficiary {
  id: number;
  bankCode: string;
  accountNumber: string;
  accountName: string;
  nickname?: string;
  createdAt: string;
}

export interface BeneficiaryRequest {
  bankCode: string;
  accountNumber: string;
  accountName: string;
  nickname?: string;
}

export type HoldStatus = "HELD" | "CAPTURED" | "RELEASED";

export interface BalanceHold {
  id: number;
  accountId: number;
  amount: number;
  currency: string;
  status: HoldStatus;
  reference: string;
}

export interface InternalTransferRequest {
  fromAccount: string;
  toAccount: string;
  amount: number;
  currency: string;
  description?: string;
}

export interface TransferResult {
  transactionId: number;
  status: string;
  amount: number;
  fee: number;
  currency: string;
  transactionTime: string;
}
