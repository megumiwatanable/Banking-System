import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, Observable } from 'rxjs';
import { Account, ApiError, ApiService, Asset, CasaEnrollment, CitadInquiry, CreditRating, CustomerProfile, InterbankTransferResponse, Transaction, User } from './api.service';

type Page = 'overview' | 'accounts' | 'transactions' | 'services' | 'profile';
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
  transferMode: 'internal' | 'interbank' = 'internal';
  amount: number | null = null;
  receiveAccountNumber = '';
  bankCode = '';
  recipientName = '';
  newAccountType: Account['accountType'] = 'CURRENT';
  profileForm = { fullName: '', email: '', currentPass: '', newPass: '' };
  customerProfile: CustomerProfile | null = null;
  customerForm = { phoneNumber: '', address: '', dateOfBirth: '' };
  casa: CasaEnrollment | null = null;
  casaPackage: 'BASIC' | 'PREMIUM' = 'BASIC';
  creditRating: CreditRating | null = null;
  assets: Asset[] = [];
  assetForm = { assetType: 'CASH', assetName: '', estimatedValue: 0, currency: 'VND', description: '' };
  citadInquiries: CitadInquiry[] = [];
  citadForm = { transactionId: 0, reason: '' };
  busy = false;
  today = new Date();
  loading = false;
  error = '';
  notice = '';

  ngOnInit() { if (sessionStorage.getItem('banking_token')) this.load(); }

  get selectedAccount() { return this.accounts.find(a => a.id === this.selectedAccountId) ?? this.accounts[0]; }
  get currentAccounts() { return this.accounts.filter(a => a.accountType === 'CURRENT'); }
  get totalBalance() { return this.accounts.reduce((sum, account) => sum + Number(account.balance), 0); }
  get displayedTransactions() {
    const number = this.selectedAccount?.accountNumber;
    return this.page === 'transactions' && number
      ? this.transactions.filter(t => t.senderAccountNumber === number || t.receiverAccountNumber === number)
      : this.transactions;
  }
  get income() { return this.transactions.filter(t => t.status === 'SUCCESS' && this.isIncoming(t)).reduce((sum, t) => sum + Number(t.amount), 0); }
  get outgoing() { return this.transactions.filter(t => t.status === 'SUCCESS' && !this.isIncoming(t)).reduce((sum, t) => sum + Number(t.amount), 0); }

  isIncoming(t: Transaction) { return t.transactionType === 'DEPOSIT' || (t.transactionType === 'TRANSFER' && this.accounts.some(a => a.accountNumber === t.receiverAccountNumber) && !this.accounts.some(a => a.accountNumber === t.senderAccountNumber)); }
  transactionLabel(t: Transaction) { return t.transactionType === 'DEPOSIT' ? 'Nạp tiền' : t.transactionType === 'WITHDRAW' ? 'Rút tiền' : t.transactionType === 'INTERBANK_TRANSFER' ? 'Yêu cầu liên ngân hàng' : this.isIncoming(t) ? 'Nhận chuyển khoản' : 'Chuyển tiền'; }
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
    if (this.action === 'transfer' && !this.receiveAccountNumber.trim()) { this.error = 'Nhập số tài khoản nhận.'; return; }
    if (this.action === 'transfer' && this.transferMode === 'internal' && this.receiveAccountNumber.trim() === this.selectedAccount?.accountNumber) { this.error = 'Tài khoản nhận phải khác tài khoản gửi.'; return; }
    if (this.action === 'transfer' && this.transferMode === 'interbank' && (!this.bankCode.trim() || !this.recipientName.trim())) { this.error = 'Nhập ngân hàng và tên người nhận.'; return; }
    this.busy = true;
    const label = this.action === 'deposit' ? 'Nạp tiền' : this.action === 'withdraw' ? 'Rút tiền' : 'Chuyển tiền';
    const request: Observable<Account | InterbankTransferResponse> = this.action === 'transfer'
      ? this.transferMode === 'internal'
        ? this.api.transfer(this.selectedAccount!.accountNumber, this.receiveAccountNumber.trim(), this.amount)
        : this.api.interbankTransfer(this.selectedAccount!.accountNumber, this.bankCode.trim(), this.receiveAccountNumber.trim(), this.recipientName.trim(), this.amount)
      : this.api.money(this.selectedAccountId, this.action, this.amount);
    request.subscribe({
      next: () => { this.busy = false; this.action = null; this.amount = null; this.receiveAccountNumber = ''; this.notice = this.transferMode === 'interbank' && label === 'Chuyển tiền' ? 'Đã ghi nhận yêu cầu mô phỏng. Chưa trừ tiền và chưa gửi đến ngân hàng nhận.' : `${label} thành công.`; this.load(); },
      error: (e: HttpErrorResponse) => this.fail(e)
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
  loadServices() {
    this.api.customerProfile().subscribe(p => { this.customerProfile = p; this.customerForm = { phoneNumber: p.phoneNumber || '', address: p.address || '', dateOfBirth: p.dateOfBirth || '' }; });
    this.api.assets().subscribe(items => this.assets = items);
    this.api.citadInquiries().subscribe(items => this.citadInquiries = items);
    this.api.casa().subscribe({ next: value => this.casa = value, error: e => { if (e.status !== 404) this.fail(e); } });
    this.api.creditRating().subscribe({ next: value => this.creditRating = value, error: e => { if (e.status !== 404) this.fail(e); } });
  }
  saveCustomerProfile() { this.api.updateCustomerProfile(this.customerForm.phoneNumber, this.customerForm.address, this.customerForm.dateOfBirth).subscribe({ next: p => { this.customerProfile=p; this.notice='Đã cập nhật hồ sơ khách hàng.'; }, error:e=>this.fail(e) }); }
  enrollCasa() { if (!this.selectedAccount) return; this.api.enrollCasa(this.selectedAccount.accountNumber,this.casaPackage).subscribe({next:v=>{this.casa=v;this.notice='Đăng ký CASA thành công.';},error:e=>this.fail(e)}); }
  evaluateCredit() { this.api.evaluateCredit().subscribe({next:v=>{this.creditRating=v;this.notice='Đã cập nhật xếp hạng nội bộ.';},error:e=>this.fail(e)}); }
  createAsset() { this.api.createAsset(this.assetForm).subscribe({next:v=>{this.assets=[v,...this.assets];this.assetForm={assetType:'CASH',assetName:'',estimatedValue:0,currency:'VND',description:''};this.notice='Đã thêm tài sản.';},error:e=>this.fail(e)}); }
  deleteAsset(id:number) { this.api.deleteAsset(id).subscribe({next:()=>this.assets=this.assets.filter(a=>a.id!==id),error:e=>this.fail(e)}); }
  createCitadInquiry() { this.api.createCitadInquiry(this.citadForm.transactionId,this.citadForm.reason).subscribe({next:v=>{this.citadInquiries=[v,...this.citadInquiries];this.citadForm={transactionId:0,reason:''};this.notice='Đã tiếp nhận tra soát mô phỏng.';},error:e=>this.fail(e)}); }
  show(page: Page) { this.page = page; this.clearMessages(); if (page === 'services') { this.selectedAccountId = this.currentAccounts[0]?.id ?? this.selectedAccountId; this.loadServices(); } }
  openAction(action: Action) { this.action = action; this.transferMode = 'internal'; this.amount = null; this.receiveAccountNumber = ''; this.bankCode = ''; this.recipientName = ''; this.clearMessages(); }
  logout() { sessionStorage.removeItem('banking_token'); this.user = null; this.accounts = []; this.transactions = []; this.page = 'overview'; this.action = null; }
  private clearMessages() { this.error = ''; this.notice = ''; }
  private fail(error: HttpErrorResponse) { this.busy = false; this.error = (error.error as ApiError)?.validationErrors?.join(' · ') || (error.error as ApiError)?.message || 'Không thể kết nối máy chủ. Vui lòng thử lại.'; }
}
