import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { BalanceHold } from "../models/banking-operation.models";

@Injectable({ providedIn: "root" })
export class HoldApiService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<BalanceHold[]>("/api/holds");
  }

  create(accountId: number, amount: number, currency: string) {
    return this.http.post<BalanceHold>("/api/holds", {
      accountId,
      amount,
      currency,
      reference: crypto.randomUUID(),
    });
  }

  release(holdId: number) {
    return this.http.post<BalanceHold>(`/api/holds/${holdId}/release`, {});
  }

  capture(holdId: number) {
    return this.http.post<BalanceHold>(`/api/holds/${holdId}/capture`, {});
  }
}
