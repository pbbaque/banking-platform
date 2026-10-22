import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  finalize,
  forkJoin,
  of,
  switchMap,
} from 'rxjs';

import {
  AccountResponse,
  AccountService,
} from '../../../core/api/account';
import { CustomerService } from '../../../core/api/customer';
import {
  TransferResponse,
  TransferService,
} from '../../../core/api/transfer';

@Component({
  selector: 'app-transfer-list',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './transfer-list.html',
  styleUrl: './transfer-list.scss',
})
export class TransferList implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly accountService = inject(AccountService);
  private readonly transferService = inject(TransferService);

  readonly accounts = signal<AccountResponse[]>([]);
  readonly transfers = signal<TransferResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly ownedAccountIds = computed(
    () => new Set(this.accounts().map((account) => account.id)),
  );

  readonly completedTransfers = computed(
    () =>
      this.transfers().filter(
        (transfer) => transfer.status === 'COMPLETED',
      ).length,
  );

  readonly pendingTransfers = computed(
    () =>
      this.transfers().filter(
        (transfer) => transfer.status === 'PENDING',
      ).length,
  );

  readonly failedTransfers = computed(
    () =>
      this.transfers().filter(
        (transfer) => transfer.status === 'FAILED',
      ).length,
  );

  ngOnInit(): void {
    this.loadTransfers();
  }

  loadTransfers(): void {
    this.loading.set(true);
    this.error.set(null);

    this.customerService
      .getCurrentCustomer()
      .pipe(
        switchMap((customer) =>
          this.accountService.getByCustomerId(customer.id),
        ),
        switchMap((accounts) => {
          this.accounts.set(accounts);

          if (accounts.length === 0) {
            return of([] as TransferResponse[][]);
          }

          return forkJoin(
            accounts.map((account) =>
              this.transferService.getByAccountId(account.id),
            ),
          );
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (transferGroups) => {
          const transfers = transferGroups.flat();

          const uniqueTransfers = Array.from(
            new Map(
              transfers.map((transfer) => [
                transfer.id,
                transfer,
              ]),
            ).values(),
          );

          uniqueTransfers.sort(
            (first, second) =>
              new Date(second.createdAt).getTime() -
              new Date(first.createdAt).getTime(),
          );

          this.transfers.set(uniqueTransfers);
        },
        error: () => {
          this.error.set(
            'No hemos podido cargar tus transferencias. Inténtalo de nuevo.',
          );
        },
      });
  }

  direction(
    transfer: TransferResponse,
  ): 'SENT' | 'RECEIVED' {
    return this.ownedAccountIds().has(transfer.sourceAccountId)
      ? 'SENT'
      : 'RECEIVED';
  }

  directionLabel(transfer: TransferResponse): string {
    return this.direction(transfer) === 'SENT'
      ? 'Enviada'
      : 'Recibida';
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

  accountLabel(accountId: string): string {
    const account = this.accounts().find(
      (item) => item.id === accountId,
    );

    if (!account) {
      return this.shortId(accountId);
    }

    return this.maskedIban(account.iban);
  }

  counterpartyLabel(transfer: TransferResponse): string {
    const accountId =
      this.direction(transfer) === 'SENT'
        ? transfer.destinationAccountId
        : transfer.sourceAccountId;

    return this.accountLabel(accountId);
  }

  signedAmount(transfer: TransferResponse): number {
    return this.direction(transfer) === 'SENT'
      ? -Number(transfer.amount)
      : Number(transfer.amount);
  }

  pendingTotalBalance(
    transfer: TransferResponse,
  ): number | null {
    if (
      transfer.status !== 'PENDING' ||
      transfer.balanceAfter === null
    ) {
      return null;
    }

    return (
      Number(transfer.balanceAfter) +
      Number(transfer.amount)
    );
  }

  private maskedIban(iban: string): string {
    if (iban.length <= 8) {
      return iban;
    }

    return `${iban.slice(0, 4)} •••• ${iban.slice(-4)}`;
  }

  private shortId(id: string): string {
    return `${id.slice(0, 8)}…${id.slice(-4)}`;
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