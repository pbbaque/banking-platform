import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  finalize,
  forkJoin,
  switchMap,
} from 'rxjs';

import {
  AccountResponse,
  AccountService,
} from '../../../core/api/account';
import { CustomerService } from '../../../core/api/customer';
import {
  TransferDetailResponse,
  TransferService,
} from '../../../core/api/transfer';

@Component({
  selector: 'app-transfer-detail',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './transfer-detail.html',
  styleUrl: './transfer-detail.scss',
})
export class TransferDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly customerService = inject(CustomerService);
  private readonly accountService = inject(AccountService);
  private readonly transferService = inject(TransferService);

  readonly accounts = signal<AccountResponse[]>([]);
  readonly transfer = signal<TransferDetailResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly ownedAccountIds = computed(
    () => new Set(this.accounts().map((account) => account.id)),
  );

  constructor() {
    this.loadTransfer();
  }

  loadTransfer(): void {
    const transferId = this.route.snapshot.paramMap.get('id');

    if (!transferId) {
      this.error.set(
        'No se ha podido identificar la transferencia solicitada.',
      );
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.customerService
      .getCurrentCustomer()
      .pipe(
        switchMap((customer) =>
          forkJoin({
            accounts: this.accountService.getByCustomerId(
              customer.id,
            ),
            transfer: this.transferService.getById(
              transferId,
            ),
          }),
        ),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: ({ accounts, transfer }) => {
          this.accounts.set(accounts);
          this.transfer.set(transfer);
        },
        error: () => {
          this.error.set(
            'No hemos podido cargar los datos de la transferencia.',
          );
        },
      });
  }

  direction(
    transfer: TransferDetailResponse,
  ): 'SENT' | 'RECEIVED' {
    return this.ownedAccountIds().has(
      transfer.sourceAccountId,
    )
      ? 'SENT'
      : 'RECEIVED';
  }

  directionLabel(
    transfer: TransferDetailResponse,
  ): string {
    return this.direction(transfer) === 'SENT'
      ? 'Enviada'
      : 'Recibida';
  }

  title(
    transfer: TransferDetailResponse,
  ): string {
    return this.direction(transfer) === 'SENT'
      ? 'Transferencia enviada'
      : 'Transferencia recibida';
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'Completada';

      case 'FAILED':
        return 'Fallida';

      case 'PENDING':
        return 'Pendiente';

      default:
        return this.formatEnum(status);
    }
  }

  signedAmount(
    transfer: TransferDetailResponse,
  ): number {
    return this.direction(transfer) === 'SENT'
      ? -Number(transfer.amount)
      : Number(transfer.amount);
  }

  sourceOwnerLabel(
    transfer: TransferDetailResponse,
  ): string {
    return this.ownedAccountIds().has(
      transfer.sourceAccountId,
    )
      ? 'Tu cuenta'
      : 'Otra cuenta';
  }

  destinationOwnerLabel(
    transfer: TransferDetailResponse,
  ): string {
    return this.ownedAccountIds().has(
      transfer.destinationAccountId,
    )
      ? 'Tu cuenta'
      : 'Otra cuenta';
  }

  isOutgoingPending(
    transfer: TransferDetailResponse,
  ): boolean {
    return (
      transfer.status === 'PENDING' &&
      this.direction(transfer) === 'SENT' &&
      transfer.balanceAfter !== null
    );
  }

  pendingTotalBalance(
    transfer: TransferDetailResponse,
  ): number | null {
    if (!this.isOutgoingPending(transfer)) {
      return null;
    }

    return (
      Number(transfer.balanceAfter) +
      Number(transfer.amount)
    );
  }

  private formatEnum(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map(
        (word) =>
          word.charAt(0).toUpperCase() + word.slice(1),
      )
      .join(' ');
  }
}