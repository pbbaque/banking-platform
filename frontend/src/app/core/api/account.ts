import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface AccountResponse {
    id: string;
    customerId: string;
    iban: string;
    accountType: string;
    currency: string;
    balance: number;
    status: string;
    version: number;
    createdAt: string;
    updatedAt: string;
    closedAt: string | null;
}

@Injectable({
    providedIn: 'root',
})
export class AccountService {
    private readonly http = inject(HttpClient);

    getByCustomerId(customerId: string): Observable<AccountResponse[]> {
        const params = new HttpParams().set('customerId', customerId);

        return this.http.get<AccountResponse[]>(
            '/account-api/api/accounts',
            { params },
        );
    }

    getById(accountId: string): Observable<AccountResponse> {
        return this.http.get<AccountResponse>(
            `/account-api/api/accounts/${accountId}`,
        );
    }

    getByIban(iban: string): Observable<AccountResponse> {
        return this.http.get<AccountResponse>(
            `/account-api/api/accounts/iban/${encodeURIComponent(iban)}`,
        );
    }
}