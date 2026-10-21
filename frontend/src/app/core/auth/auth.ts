import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
    username: string;
    password: string;
}

export interface LoginResponse {
    accessToken: string;
}

@Injectable({
    providedIn: 'root',
})
export class Auth {
    private static readonly TOKEN_KEY = 'banking_access_token';

    private readonly http = inject(HttpClient);

    private readonly tokenSignal = signal<string | null>(
        localStorage.getItem(Auth.TOKEN_KEY),
    );

    readonly token = this.tokenSignal.asReadonly();

    readonly authenticated = computed(() => {
        const token = this.tokenSignal();

        if (!token) {
            return false;
        }

        return !this.isExpired(token);
    });

    login(request: LoginRequest): Observable<LoginResponse> {
        return this.http
            .post<LoginResponse>('/identity-api/api/v1/auth/login', request)
            .pipe(
                tap((response) => {
                    localStorage.setItem(Auth.TOKEN_KEY, response.accessToken);
                    this.tokenSignal.set(response.accessToken);
                }),
            );
    }

    logout(): void {
        localStorage.removeItem(Auth.TOKEN_KEY);
        this.tokenSignal.set(null);
    }

    getAccessToken(): string | null {
        const token = this.tokenSignal();

        if (!token) {
            return null;
        }

        if (this.isExpired(token)) {
            this.logout();
            return null;
        }

        return token;
    }

    private isExpired(token: string): boolean {
        try {
            const payload = token.split('.')[1];

            if (!payload) {
                return true;
            }

            const normalizedPayload = payload
                .replace(/-/g, '+')
                .replace(/_/g, '/');

            const decodedPayload = JSON.parse(atob(normalizedPayload)) as {
                exp?: number;
            };

            if (!decodedPayload.exp) {
                return false;
            }

            return decodedPayload.exp * 1000 <= Date.now();
        } catch {
            return true;
        }
    }
}