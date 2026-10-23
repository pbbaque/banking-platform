import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface AuditEventResponse {
    id: string;
    eventType: string;
    aggregateType: string;
    aggregateId: string;
    actorId: string | null;
    outcome: string;
    description: string;
    metadata: string | null;
    occurredAt: string;
    createdAt: string;
}

@Injectable({
    providedIn: 'root',
})
export class AuditService {
    private readonly http = inject(HttpClient);

    getById(
        eventId: string,
    ): Observable<AuditEventResponse> {
        return this.http.get<AuditEventResponse>(
            `/audit-api/api/audit-events/${eventId}`,
        );
    }

    getByActor(
        actorId: string,
    ): Observable<AuditEventResponse[]> {
        const params = new HttpParams().set(
            'actorId',
            actorId,
        );

        return this.http.get<AuditEventResponse[]>(
            '/audit-api/api/audit-events',
            { params },
        );
    }

    getByEventType(
        eventType: string,
    ): Observable<AuditEventResponse[]> {
        const params = new HttpParams().set(
            'eventType',
            eventType,
        );

        return this.http.get<AuditEventResponse[]>(
            '/audit-api/api/audit-events',
            { params },
        );
    }

    getByAggregate(
        aggregateType: string,
        aggregateId: string,
    ): Observable<AuditEventResponse[]> {
        const params = new HttpParams()
            .set('aggregateType', aggregateType)
            .set('aggregateId', aggregateId);

        return this.http.get<AuditEventResponse[]>(
            '/audit-api/api/audit-events',
            { params },
        );
    }
}