import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import {
  Beneficiary,
  BeneficiaryRequest,
} from "../models/banking-operation.models";

@Injectable({ providedIn: "root" })
export class BeneficiaryApiService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<Beneficiary[]>("/api/beneficiaries");
  }

  create(request: BeneficiaryRequest) {
    return this.http.post<Beneficiary>("/api/beneficiaries", request);
  }

  update(beneficiaryId: number, request: BeneficiaryRequest) {
    return this.http.put<Beneficiary>(
      `/api/beneficiaries/${beneficiaryId}`,
      request,
    );
  }

  delete(beneficiaryId: number) {
    return this.http.delete<void>(`/api/beneficiaries/${beneficiaryId}`);
  }
}
