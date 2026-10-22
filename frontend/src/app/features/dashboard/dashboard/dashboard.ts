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
import {
  CustomerResponse,
  CustomerService,
} from '../../../core/api/customer';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly accountService = inject(AccountService);

  readonly customer = signal<CustomerResponse | null>(null);
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

  readonly firstName = computed(
    () => this.customer()?.firstName ?? '',
  );

  readonly customerName = computed(() => {
    const customer = this.customer();

    if (!customer) {
      return '';
    }

    return `${customer.firstName} ${customer.lastName}`.trim();
  });

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);

    this.customerService
      .getCurrentCustomer()
      .pipe(
        switchMap((customer) => {
          this.customer.set(customer);

          return this.accountService.getByCustomerId(customer.id);
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (accounts) => {
          this.accounts.set(accounts);
        },
        error: () => {
          this.error.set(
            'No hemos podido cargar tu información bancaria. Inténtalo de nuevo.',
          );
        },
      });
  }

  accountTypeLabel(accountType: string): string {
    return this.formatEnum(accountType);
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