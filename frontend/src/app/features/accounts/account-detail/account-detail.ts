import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import {
  AccountResponse,
  AccountService,
} from '../../../core/api/account';

@Component({
  selector: 'app-account-detail',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './account-detail.html',
  styleUrl: './account-detail.scss',
})
export class AccountDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly accountService = inject(AccountService);

  readonly account = signal<AccountResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly accountTypeLabel = computed(() => {
    const account = this.account();

    if (!account) {
      return '';
    }

    switch (account.accountType) {
      case 'CHECKING':
        return 'Cuenta corriente';
      case 'SAVINGS':
        return 'Cuenta de ahorro';
      default:
        return this.formatEnum(account.accountType);
    }
  });

  readonly statusLabel = computed(() => {
    const account = this.account();

    if (!account) {
      return '';
    }

    switch (account.status) {
      case 'ACTIVE':
        return 'Activa';
      case 'BLOCKED':
        return 'Bloqueada';
      case 'CLOSED':
        return 'Cerrada';
      default:
        return this.formatEnum(account.status);
    }
  });

  ngOnInit(): void {
    this.loadAccount();
  }

  loadAccount(): void {
    const accountId = this.route.snapshot.paramMap.get('id');

    if (!accountId) {
      this.loading.set(false);
      this.error.set('No se ha podido identificar la cuenta.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.accountService
      .getById(accountId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (account) => {
          this.account.set(account);
        },
        error: () => {
          this.error.set(
            'No hemos podido cargar los detalles de la cuenta. Inténtalo de nuevo.',
          );
        },
      });
  }

  formattedIban(): string {
    const iban = this.account()?.iban ?? '';

    return iban.replace(/(.{4})/g, '$1 ').trim();
  }

  private formatEnum(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }
}