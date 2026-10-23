import { DatePipe } from '@angular/common';
import {
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  finalize,
  switchMap,
} from 'rxjs';

import {
  AuditEventResponse,
  AuditService,
} from '../../../core/api/audit';
import { CustomerService } from '../../../core/api/customer';

type AuditFilter =
  | 'ALL'
  | 'SUCCESS'
  | 'FAILURE';

@Component({
  selector: 'app-audit-list',
  imports: [
    DatePipe,
    RouterLink,
  ],
  templateUrl: './audit-list.html',
  styleUrl: './audit-list.scss',
})
export class AuditList {
  private readonly auditService =
    inject(AuditService);

  private readonly customerService =
    inject(CustomerService);

  readonly events =
    signal<AuditEventResponse[]>([]);

  readonly loading = signal(true);

  readonly error =
    signal<string | null>(null);

  readonly selectedFilter =
    signal<AuditFilter>('ALL');

  readonly expandedEventId =
    signal<string | null>(null);

  readonly sortedEvents = computed(() =>
    [...this.events()].sort(
      (left, right) =>
        new Date(right.occurredAt).getTime() -
        new Date(left.occurredAt).getTime(),
    ),
  );

  readonly filteredEvents = computed(() => {
    const filter = this.selectedFilter();

    if (filter === 'ALL') {
      return this.sortedEvents();
    }

    return this.sortedEvents().filter(
      (event) =>
        this.normalizedOutcome(event) === filter,
    );
  });

  readonly successCount = computed(
    () =>
      this.events().filter(
        (event) =>
          this.normalizedOutcome(event) ===
          'SUCCESS',
      ).length,
  );

  readonly failureCount = computed(
    () =>
      this.events().filter(
        (event) =>
          this.normalizedOutcome(event) ===
          'FAILURE',
      ).length,
  );

  constructor() {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading.set(true);
    this.error.set(null);

    this.customerService
      .getCurrentCustomer()
      .pipe(
        switchMap((customer) =>
          this.auditService.getByActor(
            customer.identityUserId,
          ),
        ),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (events) => {
          this.events.set(events);
        },
        error: () => {
          this.error.set(
            'No hemos podido cargar tu actividad.',
          );
        },
      });
  }

  setFilter(filter: AuditFilter): void {
    this.selectedFilter.set(filter);
  }

  toggleDetails(eventId: string): void {
    this.expandedEventId.update(
      (current) =>
        current === eventId
          ? null
          : eventId,
    );
  }

  isExpanded(eventId: string): boolean {
    return (
      this.expandedEventId() === eventId
    );
  }

  eventTitle(
    event: AuditEventResponse,
  ): string {
    switch (event.eventType) {
      case 'TRANSFER_COMPLETED':
        return 'Transferencia completada';

      case 'TRANSFER_FAILED':
        return 'Transferencia fallida';

      default:
        return this.formatEnum(
          event.eventType,
        );
    }
  }

  eventDescription(
    event: AuditEventResponse,
  ): string {
    if (event.description) {
      return event.description;
    }

    switch (event.eventType) {
      case 'TRANSFER_COMPLETED':
        return 'La transferencia se procesó correctamente.';

      case 'TRANSFER_FAILED':
        return 'La transferencia no pudo completarse.';

      default:
        return 'Actividad registrada en la cuenta.';
    }
  }

  outcomeLabel(
    event: AuditEventResponse,
  ): string {
    const outcome =
      this.normalizedOutcome(event);

    switch (outcome) {
      case 'SUCCESS':
        return 'Correcta';

      case 'FAILURE':
        return 'Fallida';

      default:
        return this.formatEnum(
          event.outcome,
        );
    }
  }

  aggregateLabel(
    aggregateType: string,
  ): string {
    switch (aggregateType) {
      case 'TRANSFER':
        return 'Transferencia';

      case 'ACCOUNT':
        return 'Cuenta';

      case 'CUSTOMER':
        return 'Cliente';

      case 'IDENTITY':
        return 'Identidad';

      default:
        return this.formatEnum(
          aggregateType,
        );
    }
  }

  metadataEntries(
    event: AuditEventResponse,
  ): Array<{
    key: string;
    value: string;
  }> {
    if (!event.metadata) {
      return [];
    }

    try {
      const parsed = JSON.parse(
        event.metadata,
      ) as Record<string, unknown>;

      return Object.entries(parsed).map(
        ([key, value]) => ({
          key: this.metadataLabel(key),
          value: this.formatMetadataValue(
            value,
          ),
        }),
      );
    } catch {
      return [
        {
          key: 'Información',
          value: event.metadata,
        },
      ];
    }
  }

  hasMetadata(
    event: AuditEventResponse,
  ): boolean {
    return (
      this.metadataEntries(event).length > 0
    );
  }

  transferRoute(
    event: AuditEventResponse,
  ): string[] | null {
    if (
      event.aggregateType !== 'TRANSFER'
    ) {
      return null;
    }

    return [
      '/transfers',
      event.aggregateId,
    ];
  }

  private normalizedOutcome(
    event: AuditEventResponse,
  ): AuditFilter {
    const outcome =
      event.outcome.toUpperCase();

    if (
      outcome === 'SUCCESS' ||
      outcome === 'SUCCEEDED' ||
      outcome === 'COMPLETED'
    ) {
      return 'SUCCESS';
    }

    if (
      outcome === 'FAILURE' ||
      outcome === 'FAILED' ||
      outcome === 'ERROR'
    ) {
      return 'FAILURE';
    }

    if (
      event.eventType.endsWith(
        '_COMPLETED',
      )
    ) {
      return 'SUCCESS';
    }

    if (
      event.eventType.endsWith(
        '_FAILED',
      )
    ) {
      return 'FAILURE';
    }

    return 'ALL';
  }

  private metadataLabel(
    key: string,
  ): string {
    const labels: Record<string, string> = {
      sourceAccountId:
        'Cuenta de origen',
      destinationAccountId:
        'Cuenta de destino',
      amount: 'Importe',
      currency: 'Moneda',
      status: 'Estado',
      failureReason:
        'Motivo del fallo',
      idempotencyKey:
        'Clave de idempotencia',
    };

    return (
      labels[key] ??
      this.formatEnum(key)
    );
  }

  private formatMetadataValue(
    value: unknown,
  ): string {
    if (
      value === null ||
      value === undefined
    ) {
      return '—';
    }

    if (typeof value === 'object') {
      return JSON.stringify(value);
    }

    return String(value);
  }

  private formatEnum(
    value: string,
  ): string {
    return value
      .replace(
        /([a-z])([A-Z])/g,
        '$1 $2',
      )
      .toLowerCase()
      .split('_')
      .flatMap((part) =>
        part.split(' '),
      )
      .filter(Boolean)
      .map(
        (word) =>
          word.charAt(0).toUpperCase() +
          word.slice(1),
      )
      .join(' ');
  }
}