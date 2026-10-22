import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface CustomerResponse {
    id: string;
    identityUserId: string;
    firstName: string;
    lastName: string;
    documentType: string;
    documentNumber: string;
    dateOfBirth: string;
    phoneNumber: string;
    fiscalAddress: string;
    postalCode: string;
    city: string;
    countryCode: string;
    status: string;
    createdAt: string;
    updatedAt: string;
}

@Injectable({
    providedIn: 'root',
})
export class CustomerService {
    private readonly http = inject(HttpClient);

    getCurrentCustomer(): Observable<CustomerResponse> {
        return this.http.get<CustomerResponse>(
            '/customer-api/api/v1/customers/me',
        );
    }

    getById(customerId: string): Observable<CustomerResponse> {
        return this.http.get<CustomerResponse>(
            `/customer-api/api/v1/customers/${customerId}`,
        );
    }
}