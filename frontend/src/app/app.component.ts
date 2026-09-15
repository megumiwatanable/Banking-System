import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { Account, ApiError, ApiService, Transaction, User } from './api.service';

type Page = 'overview' | 'accounts' | 'transactions' | 'profile';
type Action = 'deposit' | 'withdraw' | 'transfer';

@Component({ selector: 'app-root', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './app.component.html' })
export class AppComponent implements OnInit {
  private api = inject(ApiService);
  page: Page = 'overview';
  authMode: 'login' | 'register' = 'login';
  auth = { fullName: '', email: '', password: '' };
  user: User | null = null;
  accounts: Account[] = [];
  transactions: Transaction[] = [];
  selectedAccountId: number | null = null;
  action: Action | null = null;
  amount: number | null = null;
  receiveAccountId: number | null = null;
  newAccountType: Account['accountType'] = 'CURRENT';
  profileForm = { fullName: '', email: '', currentPass: '', newPass: '' };
  busy = false;
  today = new Date();
  loading = false;
  error = '';
  notice = '';

  ngOnInit() { if (sessionStorage.getItem('banking_token')) this.load(); }

  get selectedAccount() { return this.accounts.find(a => a.id === this.selectedAccountId) ?? this.accounts[0]; }
  get totalBalance() { return this.accounts.reduce((sum, account) => sum + Number(account.balance), 0); }
  get displayedTransactions() {
    const number = this.selectedAccount?.accountNumber;
    return this.page === 'transactions' && number
      ? this.transactions.filter(t => t.senderAccountNumber === number || t.receiverAccountNumber === number)
      : this.transactions;
  }
  get income() { return this.transactions.filter(t => this.isIncoming(t)).reduce((sum, t) => sum + Number(t.amount), 0); }
  get outgoing() { return this.transactions.filter(t => !this.isIncoming(t)).reduce((sum, t) => sum + Number(t.amount), 0); }

  isIncoming(t: Transaction) { return t.transactionType === 'DEPOSIT' || (t.transactionType === 'TRANSFER' && this.accounts.some(a => a.accountNumber === t.receiverAccountNumber) && !this.accounts.some(a => a.accountNumber === t.senderAccountNumber)); }
  transactionLabel(t: Transaction) { return t.transactionType === 'DEPOSIT' ? 'Nạp tiền' : t.transactionType === 'WITHDRAW' ? 'Rút tiền' : this.isIncoming(t) ? 'Nhận chuyển khoản' : 'Chuyển tiền'; }
  initials(name: string) { return name.split(' ').slice(-2).map(part => part[0]?.toUpperCase()).join(''); }

  authenticate() {
    this.clearMessages();
    if (!this.auth.email || !this.auth.password || (this.authMode === 'register' && !this.auth.fullName)) { this.error = 'Vui lòng điền đầy đủ thông tin.'; return; }
    this.busy = true;
    if (this.authMode === 'register') {
      this.api.register(this.auth.fullName, this.auth.email, this.auth.password).subscribe({
        next: () => { this.busy = false; this.authMode = 'login'; this.auth.password = ''; this.notice = 'Tạo tài khoản thành công. Hãy đăng nhập.'; },
        error: e => this.fail(e)
      });
      return;
    }
    this.api.login(this.auth.email, this.auth.password).subscribe({
      next: result => { sessionStorage.setItem('banking_token', result.token); this.auth.password = ''; this.busy = false; this.load(); },
      error: e => this.fail(e)
    });
  }

  load() {
    this.loading = true;
    forkJoin({ user: this.api.profile(), accounts: this.api.accounts(), transactions: this.api.transactions() }).subscribe({
      next: data => {
        this.user = data.user;
        this.accounts = data.accounts;
        this.transactions = data.transactions.sort((a, b) => b.transactionTime.localeCompare(a.transactionTime));
        if (!this.accounts.some(a => a.id === this.selectedAccountId)) this.selectedAccountId = this.accounts[0]?.id ?? null;
        this.profileForm.fullName = data.user.fullName;
        this.profileForm.email = data.user.email;
        this.loading = false;
      },
      error: e => { this.loading = false; if (e.status === 401 || e.status === 403) this.logout(); else this.fail(e); }
    });
  }

  createAccount() {
    this.clearMessages(); this.busy = true;
    this.api.createAccount(this.newAccountType).subscribe({ next: account => { this.busy = false; this.selectedAccountId = account.id; this.notice = 'Đã tạo tài khoản mới.'; this.load(); }, error: e => this.fail(e) });
  }

  submitAction() {
    if (!this.action || !this.selectedAccountId) return;
    this.clearMessages();
    if (!this.amount || this.amount <= 0 || !Number.isFinite(this.amount)) { this.error = 'Số tiền phải lớn hơn 0.'; return; }
    if (this.action === 'transfer' && (!this.receiveAccountId || this.receiveAccountId === this.selectedAccountId)) { this.error = 'Nhập ID tài khoản nhận khác tài khoản gửi.'; return; }
    this.busy = true;
    const label = this.action === 'deposit' ? 'Nạp tiền' : this.action === 'withdraw' ? 'Rút tiền' : 'Chuyển tiền';
    this.api.money(this.selectedAccountId, this.action, this.amount, this.receiveAccountId ?? undefined).subscribe({
      next: () => { this.busy = false; this.action = null; this.amount = null; this.receiveAccountId = null; this.notice = `${label} thành công.`; this.load(); },
      error: e => this.fail(e)
    });
  }

  saveProfile() {
    this.clearMessages(); this.busy = true;
    const emailChanged = this.profileForm.email !== this.user?.email;
    this.api.updateProfile(this.profileForm.fullName, this.profileForm.email).subscribe({ next: user => { this.busy = false; if (emailChanged) { this.logout(); this.notice = 'Email đã đổi. Vui lòng đăng nhập lại bằng email mới.'; } else { this.user = user; this.notice = 'Đã cập nhật hồ sơ.'; } }, error: e => this.fail(e) });
  }
  savePassword() {
    this.clearMessages(); this.busy = true;
    this.api.changePassword(this.profileForm.currentPass, this.profileForm.newPass).subscribe({ next: () => { this.busy = false; this.profileForm.currentPass = ''; this.profileForm.newPass = ''; this.notice = 'Đã đổi mật khẩu.'; }, error: e => this.fail(e) });
  }
  show(page: Page) { this.page = page; this.clearMessages(); }
  openAction(action: Action) { this.action = action; this.amount = null; this.receiveAccountId = null; this.clearMessages(); }
  logout() { sessionStorage.removeItem('banking_token'); this.user = null; this.accounts = []; this.transactions = []; this.page = 'overview'; this.action = null; }
  private clearMessages() { this.error = ''; this.notice = ''; }
  private fail(error: HttpErrorResponse) { this.busy = false; this.error = (error.error as ApiError)?.validationErrors?.join(' · ') || (error.error as ApiError)?.message || 'Không thể kết nối máy chủ. Vui lòng thử lại.'; }
}
