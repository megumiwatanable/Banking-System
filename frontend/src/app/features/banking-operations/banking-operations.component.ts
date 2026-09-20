import { CommonModule } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  inject,
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Account, ApiError, Transaction } from "../../api.models";
import { BeneficiaryApiService } from "./data-access/beneficiary-api.service";
import { HoldApiService } from "./data-access/hold-api.service";
import { TransferApiService } from "./data-access/transfer-api.service";
import {
  BalanceHold,
  Beneficiary,
  BeneficiaryRequest,
} from "./models/banking-operation.models";

@Component({
  selector: "app-banking-operations",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./banking-operations.component.html",
})
export class BankingOperationsComponent implements OnInit, OnChanges {
  private readonly transferApi = inject(TransferApiService);
  private readonly beneficiaryApi = inject(BeneficiaryApiService);
  private readonly holdApi = inject(HoldApiService);

  @Input({ required: true }) accounts: Account[] = [];
  @Input({ required: true }) transactions: Transaction[] = [];
  @Output() readonly changed = new EventEmitter<void>();

  beneficiaries: Beneficiary[] = [];
  holds: BalanceHold[] = [];
  selectedBeneficiaryId = 0;
  transferForm = {
    accountId: 0,
    toAccount: "",
    amount: 0,
    description: "",
  };
  beneficiaryForm: BeneficiaryRequest = {
    bankCode: "MONETA",
    accountNumber: "",
    accountName: "",
    nickname: "",
  };
  holdForm = { accountId: 0, amount: 0 };
  busy = false;
  error = "";
  notice = "";

  ngOnInit(): void {
    this.selectDefaultAccounts();
    this.loadBeneficiaries();
    this.loadHolds();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["accounts"]) {
      this.selectDefaultAccounts();
    }
  }

  get reversibleTransfers(): Transaction[] {
    return this.transactions.filter(
      (transaction) =>
        transaction.transactionType === "INTERNAL_TRANSFER" &&
        transaction.status === "SUCCESS" &&
        this.accounts.some(
          (account) => account.accountNumber === transaction.senderAccountNumber,
        ),
    );
  }

  selectBeneficiary(): void {
    const beneficiary = this.beneficiaries.find(
      (item) => item.id === this.selectedBeneficiaryId,
    );
    if (beneficiary) {
      this.transferForm.toAccount = beneficiary.accountNumber;
      this.transferForm.description =
        beneficiary.nickname || `Chuyển tiền cho ${beneficiary.accountName}`;
    }
  }

  submitTransfer(): void {
    const source = this.accountById(this.transferForm.accountId);
    if (!source || this.transferForm.amount <= 0) {
      this.error = "Vui lòng chọn tài khoản và nhập số tiền hợp lệ.";
      return;
    }

    this.startRequest();
    this.transferApi
      .internalTransfer({
        fromAccount: source.accountNumber,
        toAccount: this.transferForm.toAccount.trim(),
        amount: this.transferForm.amount,
        currency: source.currency,
        description: this.transferForm.description.trim() || undefined,
      })
      .subscribe({
        next: (result) => {
          this.finish(`Chuyển tiền thành công · giao dịch #${result.transactionId}.`);
          this.transferForm.toAccount = "";
          this.transferForm.amount = 0;
          this.transferForm.description = "";
        },
        error: (error: HttpErrorResponse) => this.fail(error),
      });
  }

  reverseTransfer(transactionId: number): void {
    this.startRequest();
    this.transferApi.reverse(transactionId).subscribe({
      next: (result) => this.finish(`Đã đảo giao dịch bằng #${result.transactionId}.`),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  createBeneficiary(): void {
    this.startRequest();
    this.beneficiaryApi.create(this.beneficiaryForm).subscribe({
      next: (beneficiary) => {
        this.beneficiaries = [beneficiary, ...this.beneficiaries];
        this.beneficiaryForm = {
          bankCode: "MONETA",
          accountNumber: "",
          accountName: "",
          nickname: "",
        };
        this.busy = false;
        this.notice = "Đã lưu người thụ hưởng.";
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  deleteBeneficiary(beneficiaryId: number): void {
    this.beneficiaryApi.delete(beneficiaryId).subscribe({
      next: () => {
        this.beneficiaries = this.beneficiaries.filter(
          (beneficiary) => beneficiary.id !== beneficiaryId,
        );
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  createHold(): void {
    const account = this.accountById(this.holdForm.accountId);
    if (!account || this.holdForm.amount <= 0) {
      this.error = "Vui lòng chọn tài khoản và nhập số tiền giữ hợp lệ.";
      return;
    }
    this.startRequest();
    this.holdApi
      .create(account.id, this.holdForm.amount, account.currency)
      .subscribe({
        next: (hold) => {
          this.holds = [hold, ...this.holds];
          this.holdForm.amount = 0;
          this.finish(`Đã giữ tiền · mã hold #${hold.id}.`);
        },
        error: (error: HttpErrorResponse) => this.fail(error),
      });
  }

  changeHold(hold: BalanceHold, action: "capture" | "release"): void {
    this.startRequest();
    this.holdApi[action](hold.id).subscribe({
      next: (updatedHold) => {
        this.holds = this.holds.map((item) =>
          item.id === updatedHold.id ? updatedHold : item,
        );
        this.finish(action === "capture" ? "Đã thu tiền giữ." : "Đã giải phóng tiền giữ.");
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private loadBeneficiaries(): void {
    this.beneficiaryApi.list().subscribe({
      next: (beneficiaries) => (this.beneficiaries = beneficiaries),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private loadHolds(): void {
    this.holdApi.list().subscribe({
      next: (holds) => (this.holds = holds),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private selectDefaultAccounts(): void {
    const firstAccountId = this.accounts[0]?.id ?? 0;
    this.transferForm.accountId ||= firstAccountId;
    this.holdForm.accountId ||= firstAccountId;
  }

  private accountById(accountId: number): Account | undefined {
    return this.accounts.find((account) => account.id === accountId);
  }

  private startRequest(): void {
    this.busy = true;
    this.error = "";
    this.notice = "";
  }

  private finish(notice: string): void {
    this.busy = false;
    this.notice = notice;
    this.changed.emit();
  }

  private fail(error: HttpErrorResponse): void {
    this.busy = false;
    const response = error.error as ApiError | undefined;
    this.error =
      response?.validationErrors?.join(" · ") ||
      response?.message ||
      "Không thể xử lý yêu cầu.";
  }
}
