import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { createIdempotencyKey } from "../../../core/http/idempotency-key";
import {
  InternalTransferRequest,
  TransferResult,
} from "../models/banking-operation.models";

@Injectable({ providedIn: "root" })
export class TransferApiService {
  private readonly http = inject(HttpClient);

  internalTransfer(request: InternalTransferRequest) {
    const headers = new HttpHeaders({
      "Idempotency-Key": createIdempotencyKey(),
    });
    return this.http.post<TransferResult>(
      "/api/transfers/internal",
      request,
      { headers },
    );
  }

  reverse(transactionId: number) {
    return this.http.post<TransferResult>(
      `/api/transfers/${transactionId}/reversal`,
      {},
    );
  }
}
