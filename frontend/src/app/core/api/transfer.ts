import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface CreateTransferRequest {
    sourceAccountId: string;
    destinationIban: string;
    amount: number;
    currency: string;
    idempotencyKey: string;
}

export interface TransferResponse {
    id: string;
    sourceAccountId: string;
    destinationAccountId: string;
    amount: number;
    currency: string;
    status: string;
    idempotencyKey: string;
    failureReason: string | null;
    balanceAfter: number | null;
    createdAt: string;
    updatedAt: string;
    completedAt: string | null;
}

export interface TransferDetailResponse extends TransferResponse {
    sourceIban: string;
    destinationIban: string;
}

@Injectable({
    providedIn: 'root',
})
export class TransferService {
    private readonly http = inject(HttpClient);

    create(
        request: CreateTransferRequest,
    ): Observable<TransferResponse> {
        return this.http.post<TransferResponse>(
            '/transfer-api/api/transfers',
            request,
        );
    }

    getByAccountId(
        accountId: string,
    ): Observable<TransferResponse[]> {
        const params = new HttpParams().set(
            'accountId',
            accountId,
        );

        return this.http.get<TransferResponse[]>(
            '/transfer-api/api/transfers',
            { params },
        );
    }

    getById(
        transferId: string,
    ): Observable<TransferDetailResponse> {
        return this.http.get<TransferDetailResponse>(
            `/transfer-api/api/transfers/${transferId}`,
        );
    }

    getByIdempotencyKey(
        idempotencyKey: string,
    ): Observable<TransferResponse> {
        const params = new HttpParams().set(
            'idempotencyKey',
            idempotencyKey,
        );

        return this.http.get<TransferResponse>(
            '/transfer-api/api/transfers',
            { params },
        );
    }
}