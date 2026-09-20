import { CommonModule } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { Component, OnInit, inject } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { forkJoin, Observable } from "rxjs";
import {
  Account,
  ApiError,
  Asset,
  CasaEnrollment,
  CasaPackage,
  CitadInquiry,
  CreateAssetRequest,
  CreditRating,
  CustomerProfile,
  InterbankTransferResponse,
  Transaction,
  TransferResult,
  TransferMode,
  User,
} from "./api.models";
import { ApiService } from "./api.service";
import { AUTH_TOKEN_KEY } from "./auth.interceptor";
import { BankingOperationsComponent } from "./features/banking-operations/banking-operations.component";

type Page =
  | "overview"
  | "accounts"
  | "transactions"
  | "operations"
  | "services"
  | "profile";
type AccountAction = "deposit" | "withdraw" | "transfer";
type AuthMode = "login" | "register";

const EMPTY_ASSET_FORM: CreateAssetRequest = {
  assetType: "CASH",
  assetName: "",
  estimatedValue: 0,
  currency: "VND",
  description: "",
};

@Component({
  selector: "app-root",
  standalone: true,
  imports: [CommonModule, FormsModule, BankingOperationsComponent],
  templateUrl: "./app.component.html",
})
export class AppComponent implements OnInit {
  private readonly api = inject(ApiService);

  page: Page = "overview";
  authMode: AuthMode = "login";
  auth = { fullName: "", email: "", password: "" };

  user: User | null = null;
  accounts: Account[] = [];
  transactions: Transaction[] = [];
  selectedAccountId: number | null = null;

  action: AccountAction | null = null;
  transferMode: TransferMode = "internal";
  amount: number | null = null;
  receiveAccountNumber = "";
  bankCode = "";
  recipientName = "";
  newAccountType: Account["accountType"] = "CURRENT";

  profileForm = { fullName: "", email: "", currentPass: "", newPass: "" };
  customerProfile: CustomerProfile | null = null;
  customerForm = { phoneNumber: "", address: "", dateOfBirth: "" };
  casa: CasaEnrollment | null = null;
  casaPackage: CasaPackage = "BASIC";
  creditRating: CreditRating | null = null;
  assets: Asset[] = [];
  assetForm: CreateAssetRequest = { ...EMPTY_ASSET_FORM };
  citadInquiries: CitadInquiry[] = [];
  citadForm = { transactionId: 0, reason: "" };

  busy = false;
  readonly today = new Date();
  loading = false;
  error = "";
  notice = "";

  ngOnInit(): void {
    if (sessionStorage.getItem(AUTH_TOKEN_KEY)) {
      this.loadDashboard();
    }
  }

  get selectedAccount(): Account | undefined {
    return (
      this.accounts.find((account) => account.id === this.selectedAccountId) ??
      this.accounts[0]
    );
  }

  get currentAccounts(): Account[] {
    return this.accounts.filter((account) => account.accountType === "CURRENT");
  }

  get totalBalance(): number {
    return this.accounts.reduce(
      (sum, account) => sum + Number(account.balance),
      0,
    );
  }

  get displayedTransactions(): Transaction[] {
    const number = this.selectedAccount?.accountNumber;
    return this.page === "transactions" && number
      ? this.transactions.filter(
          (transaction) =>
            transaction.senderAccountNumber === number ||
            transaction.receiverAccountNumber === number,
        )
      : this.transactions;
  }

  get income(): number {
    return this.sumSuccessfulTransactions((transaction) =>
      this.isIncoming(transaction),
    );
  }

  get outgoing(): number {
    return this.sumSuccessfulTransactions(
      (transaction) => !this.isIncoming(transaction),
    );
  }

  isIncoming(transaction: Transaction): boolean {
    if (transaction.transactionType === "DEPOSIT") {
      return true;
    }

    if (
      transaction.transactionType !== "TRANSFER" &&
      transaction.transactionType !== "INTERNAL_TRANSFER"
    ) {
      return false;
    }

    const ownsReceiver = this.ownsAccount(transaction.receiverAccountNumber);
    const ownsSender = this.ownsAccount(transaction.senderAccountNumber);
    return ownsReceiver && !ownsSender;
  }

  transactionLabel(transaction: Transaction): string {
    const labels: Partial<Record<Transaction["transactionType"], string>> = {
      DEPOSIT: "Nạp tiền",
      WITHDRAW: "Rút tiền",
      INTERNAL_TRANSFER: "Chuyển tiền nội bộ",
      INTERBANK_TRANSFER: "Yêu cầu liên ngân hàng",
      FEE: "Phí giao dịch",
      REVERSAL: "Hoàn giao dịch",
    };

    return (
      labels[transaction.transactionType] ??
      (this.isIncoming(transaction) ? "Nhận chuyển khoản" : "Chuyển tiền")
    );
  }

  initials(name: string): string {
    return name
      .trim()
      .split(/\s+/)
      .slice(-2)
      .map((part) => part[0]?.toUpperCase())
      .join("");
  }

  authenticate(): void {
    this.clearMessages();

    if (!this.hasRequiredAuthFields()) {
      this.error = "Vui lòng điền đầy đủ thông tin.";
      return;
    }

    this.busy = true;

    if (this.authMode === "register") {
      this.register();
      return;
    }

    this.login();
  }

  loadDashboard(): void {
    this.loading = true;

    forkJoin({
      user: this.api.profile(),
      accounts: this.api.accounts(),
      transactions: this.api.transactions(),
    }).subscribe({
      next: (data) => {
        this.user = data.user;
        this.accounts = data.accounts;
        this.transactions = [...data.transactions].sort((first, second) =>
          second.transactionTime.localeCompare(first.transactionTime),
        );

        if (
          !this.accounts.some(
            (account) => account.id === this.selectedAccountId,
          )
        ) {
          this.selectedAccountId = this.accounts[0]?.id ?? null;
        }

        this.profileForm.fullName = data.user.fullName;
        this.profileForm.email = data.user.email;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        if (error.status === 401 || error.status === 403) {
          this.logout();
          return;
        }
        this.fail(error);
      },
    });
  }

  createAccount(): void {
    this.startRequest();
    this.api.createAccount(this.newAccountType).subscribe({
      next: (account) => {
        this.busy = false;
        this.selectedAccountId = account.id;
        this.notice = "Đã tạo tài khoản mới.";
        this.loadDashboard();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  submitAction(): void {
    if (!this.action || !this.selectedAccountId || !this.selectedAccount) {
      return;
    }

    this.clearMessages();
    const validationError = this.validateAccountAction();
    if (validationError) {
      this.error = validationError;
      return;
    }

    this.busy = true;
    const completedAction = this.action;
    const request = this.createAccountActionRequest();

    request.subscribe({
      next: () => {
        this.busy = false;
        this.notice = this.successMessage(completedAction);
        this.resetActionForm();
        this.loadDashboard();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  saveProfile(): void {
    this.startRequest();
    const emailChanged = this.profileForm.email !== this.user?.email;

    this.api
      .updateProfile(this.profileForm.fullName, this.profileForm.email)
      .subscribe({
        next: (user) => {
          this.busy = false;
          if (emailChanged) {
            this.logout();
            this.notice =
              "Email đã đổi. Vui lòng đăng nhập lại bằng email mới.";
            return;
          }

          this.user = user;
          this.notice = "Đã cập nhật hồ sơ.";
        },
        error: (error: HttpErrorResponse) => this.fail(error),
      });
  }

  savePassword(): void {
    this.startRequest();
    this.api
      .changePassword(this.profileForm.currentPass, this.profileForm.newPass)
      .subscribe({
        next: () => {
          this.busy = false;
          this.profileForm.currentPass = "";
          this.profileForm.newPass = "";
          this.notice = "Đã đổi mật khẩu.";
        },
        error: (error: HttpErrorResponse) => this.fail(error),
      });
  }

  loadServices(): void {
    this.api.customerProfile().subscribe({
      next: (profile) => {
        this.customerProfile = profile;
        this.customerForm = {
          phoneNumber: profile.phoneNumber || "",
          address: profile.address || "",
          dateOfBirth: profile.dateOfBirth || "",
        };
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
    this.api.assets().subscribe({
      next: (assets) => (this.assets = assets),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
    this.api.citadInquiries().subscribe({
      next: (inquiries) => (this.citadInquiries = inquiries),
      error: (error: HttpErrorResponse) => this.fail(error),
    });
    this.loadOptionalServices();
  }

  saveCustomerProfile(): void {
    const { phoneNumber, address, dateOfBirth } = this.customerForm;
    this.api
      .updateCustomerProfile(phoneNumber, address, dateOfBirth)
      .subscribe({
        next: (profile) => {
          this.customerProfile = profile;
          this.notice = "Đã cập nhật hồ sơ khách hàng.";
        },
        error: (error: HttpErrorResponse) => this.fail(error),
      });
  }

  enrollCasa(): void {
    if (!this.selectedAccount) {
      return;
    }

    this.api
      .enrollCasa(this.selectedAccount.accountNumber, this.casaPackage)
      .subscribe({
        next: (enrollment) => {
          this.casa = enrollment;
          this.notice = "Đăng ký CASA thành công.";
        },
        error: (error: HttpErrorResponse) => this.fail(error),
      });
  }

  evaluateCredit(): void {
    this.api.evaluateCredit().subscribe({
      next: (rating) => {
        this.creditRating = rating;
        this.notice = "Đã cập nhật xếp hạng nội bộ.";
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  createAsset(): void {
    this.api.createAsset(this.assetForm).subscribe({
      next: (asset) => {
        this.assets = [asset, ...this.assets];
        this.assetForm = { ...EMPTY_ASSET_FORM };
        this.notice = "Đã thêm tài sản.";
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  deleteAsset(id: number): void {
    this.api.deleteAsset(id).subscribe({
      next: () => {
        this.assets = this.assets.filter((asset) => asset.id !== id);
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  createCitadInquiry(): void {
    this.api
      .createCitadInquiry(this.citadForm.transactionId, this.citadForm.reason)
      .subscribe({
        next: (inquiry) => {
          this.citadInquiries = [inquiry, ...this.citadInquiries];
          this.citadForm = { transactionId: 0, reason: "" };
          this.notice = "Đã tiếp nhận tra soát mô phỏng.";
        },
        error: (error: HttpErrorResponse) => this.fail(error),
      });
  }

  show(page: Page): void {
    this.page = page;
    this.clearMessages();

    if (page === "services") {
      this.selectedAccountId =
        this.currentAccounts[0]?.id ?? this.selectedAccountId;
      this.loadServices();
    }
  }

  openAction(action: AccountAction): void {
    this.resetActionForm();
    this.action = action;
    this.clearMessages();
  }

  logout(): void {
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    this.user = null;
    this.accounts = [];
    this.transactions = [];
    this.page = "overview";
    this.action = null;
  }

  private register(): void {
    this.api
      .register(this.auth.fullName, this.auth.email, this.auth.password)
      .subscribe({
        next: () => {
          this.busy = false;
          this.authMode = "login";
          this.auth.password = "";
          this.notice = "Tạo tài khoản thành công. Hãy đăng nhập.";
        },
        error: (error: HttpErrorResponse) => this.fail(error),
      });
  }

  private login(): void {
    this.api.login(this.auth.email, this.auth.password).subscribe({
      next: (result) => {
        sessionStorage.setItem(AUTH_TOKEN_KEY, result.token);
        this.auth.password = "";
        this.busy = false;
        this.loadDashboard();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private hasRequiredAuthFields(): boolean {
    return Boolean(
      this.auth.email &&
        this.auth.password &&
        (this.authMode === "login" || this.auth.fullName),
    );
  }

  private validateAccountAction(): string | null {
    if (!this.amount || this.amount <= 0 || !Number.isFinite(this.amount)) {
      return "Số tiền phải lớn hơn 0.";
    }

    if (this.action !== "transfer") {
      return null;
    }

    const receiver = this.receiveAccountNumber.trim();
    if (!receiver) {
      return "Nhập số tài khoản nhận.";
    }
    if (
      this.transferMode === "internal" &&
      receiver === this.selectedAccount?.accountNumber
    ) {
      return "Tài khoản nhận phải khác tài khoản gửi.";
    }
    if (
      this.transferMode === "interbank" &&
      (!this.bankCode.trim() || !this.recipientName.trim())
    ) {
      return "Nhập ngân hàng và tên người nhận.";
    }

    return null;
  }

  private createAccountActionRequest(): Observable<
    Account | InterbankTransferResponse | TransferResult
  > {
    const account = this.selectedAccount!;
    const amount = this.amount!;

    if (this.action !== "transfer") {
      return this.api.money(account.id, this.action!, amount);
    }

    if (this.transferMode === "internal") {
      return this.api.transfer(
        account.accountNumber,
        this.receiveAccountNumber.trim(),
        amount,
      );
    }

    return this.api.interbankTransfer(
      account.accountNumber,
      this.bankCode.trim(),
      this.receiveAccountNumber.trim(),
      this.recipientName.trim(),
      amount,
    );
  }

  private successMessage(action: AccountAction): string {
    if (action === "transfer" && this.transferMode === "interbank") {
      return "Đã ghi nhận yêu cầu mô phỏng. Chưa trừ tiền và chưa gửi đến ngân hàng nhận.";
    }

    const labels: Record<AccountAction, string> = {
      deposit: "Nạp tiền",
      withdraw: "Rút tiền",
      transfer: "Chuyển tiền",
    };
    return `${labels[action]} thành công.`;
  }

  private loadOptionalServices(): void {
    // A missing CASA enrollment or rating is a valid first-use state, not a UI error.
    this.api.casa().subscribe({
      next: (value) => (this.casa = value),
      error: (error: HttpErrorResponse) => this.failUnlessNotFound(error),
    });
    this.api.creditRating().subscribe({
      next: (value) => (this.creditRating = value),
      error: (error: HttpErrorResponse) => this.failUnlessNotFound(error),
    });
  }

  private resetActionForm(): void {
    this.action = null;
    this.transferMode = "internal";
    this.amount = null;
    this.receiveAccountNumber = "";
    this.bankCode = "";
    this.recipientName = "";
  }

  private sumSuccessfulTransactions(
    predicate: (transaction: Transaction) => boolean,
  ): number {
    return this.transactions
      .filter(
        (transaction) =>
          transaction.status === "SUCCESS" && predicate(transaction),
      )
      .reduce((sum, transaction) => sum + Number(transaction.amount), 0);
  }

  private ownsAccount(accountNumber: string | null): boolean {
    return this.accounts.some(
      (account) => account.accountNumber === accountNumber,
    );
  }

  private startRequest(): void {
    this.clearMessages();
    this.busy = true;
  }

  private clearMessages(): void {
    this.error = "";
    this.notice = "";
  }

  private failUnlessNotFound(error: HttpErrorResponse): void {
    if (error.status !== 404) {
      this.fail(error);
    }
  }

  private fail(error: HttpErrorResponse): void {
    this.busy = false;
    const apiError = error.error as ApiError | undefined;
    this.error =
      apiError?.validationErrors?.join(" · ") ||
      apiError?.message ||
      "Không thể kết nối máy chủ. Vui lòng thử lại.";
  }
}
