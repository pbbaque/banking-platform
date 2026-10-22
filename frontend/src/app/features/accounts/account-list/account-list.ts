import { CurrencyPipe } from '@angular/common';
import {
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs';

import {
  AccountResponse,
  AccountService,
} from '../../../core/api/account';
import { CustomerService } from '../../../core/api/customer';

@Component({
  selector: 'app-account-list',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './account-list.html',
  styleUrl: './account-list.scss',
})
export class AccountList implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly accountService = inject(AccountService);

  readonly accounts = signal<AccountResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly activeAccounts = computed(() =>
    this.accounts().filter((account) => account.status === 'ACTIVE'),
  );

  readonly totalBalance = computed(() =>
    this.activeAccounts().reduce(
      (total, account) => total + Number(account.balance),
      0,
    ),
  );

  readonly primaryCurrency = computed(
    () => this.activeAccounts()[0]?.currency ?? 'EUR',
  );

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading.set(true);
    this.error.set(null);

    this.customerService
      .getCurrentCustomer()
      .pipe(
        switchMap((customer) =>
          this.accountService.getByCustomerId(customer.id),
        ),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (accounts) => {
          this.accounts.set(accounts);
        },
        error: () => {
          this.error.set(
            'No hemos podido cargar tus cuentas. Inténtalo de nuevo.',
          );
        },
      });
  }

  accountTypeLabel(accountType: string): string {
    switch (accountType) {
      case 'CHECKING':
        return 'Cuenta corriente';
      case 'SAVINGS':
        return 'Cuenta de ahorro';
      default:
        return this.formatEnum(accountType);
    }
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'Activa';
      case 'BLOCKED':
        return 'Bloqueada';
      case 'CLOSED':
        return 'Cerrada';
      default:
        return this.formatEnum(status);
    }
  }

  maskedIban(iban: string): string {
    if (iban.length <= 8) {
      return iban;
    }

    return `${iban.slice(0, 4)} •••• •••• ${iban.slice(-4)}`;
  }

  private formatEnum(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }
}