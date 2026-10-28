import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  finalize,
  switchMap,
} from 'rxjs';

import {
  AccountResponse,
  AccountService,
} from '../../../core/api/account';
import { CustomerService } from '../../../core/api/customer';
import {
  CreateTransferRequest,
  TransferService,
} from '../../../core/api/transfer';

@Component({
  selector: 'app-transfer-create',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './transfer-create.html',
  styleUrl: './transfer-create.scss',
})
export class TransferCreate {
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly customerService = inject(CustomerService);
  private readonly accountService = inject(AccountService);
  private readonly transferService = inject(TransferService);

  readonly accounts = signal<AccountResponse[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly submitError = signal<string | null>(null);

  readonly form = this.formBuilder.group({
    sourceAccountId: ['', Validators.required],
    destinationIban: [
      '',
      [
        Validators.required,
        Validators.pattern(/^ES\d{22}$/i),
      ],
    ],
    amount: [
      null as number | null,
      [
        Validators.required,
        Validators.min(0.01),
      ],
    ],
  });

  readonly activeAccounts = computed(() =>
    this.accounts().filter(
      (account) => account.status === 'ACTIVE',
    ),
  );

  private readonly selectedAccountId = toSignal(
    this.form.controls.sourceAccountId.valueChanges,
    { initialValue: this.form.controls.sourceAccountId.value },
  );

  readonly selectedAccount = computed(
    () =>
      this.accounts().find(
        (account) =>
          account.id ===
          this.selectedAccountId(),
      ) ?? null,
  );

  constructor() {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.customerService
      .getCurrentCustomer()
      .pipe(
        switchMap((customer) =>
          this.accountService.getByCustomerId(
            customer.id,
          ),
        ),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (accounts) => {
          this.accounts.set(accounts);

          const activeAccounts = accounts.filter(
            (account) =>
              account.status === 'ACTIVE',
          );

          if (activeAccounts.length === 1) {
            this.form.controls.sourceAccountId.setValue(
              activeAccounts[0].id,
            );
          }
        },
        error: () => {
          this.loadError.set(
            'No hemos podido cargar tus cuentas.',
          );
        },
      });
  }

  selectAccount(accountId: string): void {
    this.form.controls.sourceAccountId.setValue(
      accountId,
    );

    this.form.controls.sourceAccountId.markAsTouched();
  }

  normalizeDestinationIban(): void {
    const iban =
      this.form.controls.destinationIban.value ?? '';

    const normalized = iban
      .replace(/\s+/g, '')
      .toUpperCase();

    this.form.controls.destinationIban.setValue(
      normalized,
      {
        emitEvent: false,
      },
    );
  }

  submit(): void {
    this.submitError.set(null);

    this.normalizeDestinationIban();

    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    const sourceAccount = this.selectedAccount();

    if (!sourceAccount) {
      this.submitError.set(
        'Selecciona una cuenta de origen.',
      );
      return;
    }

    const amount = Number(
      this.form.controls.amount.value,
    );

    if (!Number.isFinite(amount) || amount <= 0) {
      this.submitError.set(
        'Introduce un importe válido.',
      );
      return;
    }

    if (amount > Number(sourceAccount.balance)) {
      this.submitError.set(
        'El importe supera el saldo disponible de la cuenta.',
      );
      return;
    }

    const destinationIban =
      this.form.controls.destinationIban.value!;

    if (
      destinationIban.toUpperCase() ===
      sourceAccount.iban.toUpperCase()
    ) {
      this.submitError.set(
        'La cuenta de destino debe ser distinta de la cuenta de origen.',
      );
      return;
    }

    const request: CreateTransferRequest = {
      sourceAccountId: sourceAccount.id,
      destinationIban,
      amount,
      currency: sourceAccount.currency,
      idempotencyKey: crypto.randomUUID(),
    };

    this.submitting.set(true);

    this.transferService
      .create(request)
      .pipe(
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (transfer) => {
          void this.router.navigate([
            '/transfers',
            transfer.id,
          ]);
        },
        error: (error: unknown) => {
          this.handleSubmitError(error);
        },
      });
  }

  accountTypeLabel(type: string): string {
    switch (type) {
      case 'CHECKING':
        return 'Cuenta corriente';

      case 'SAVINGS':
        return 'Cuenta de ahorro';

      default:
        return type;
    }
  }

  formatIban(iban: string): string {
    return iban.replace(
      /(.{4})/g,
      '$1 ',
    ).trim();
  }

  private handleSubmitError(error: unknown): void {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        this.submitError.set(
          'No hemos encontrado ninguna cuenta asociada a ese IBAN.',
        );
        return;
      }

      if (error.status === 409) {
        this.submitError.set(
          'No se ha podido procesar la transferencia por un conflicto con la operación.',
        );
        return;
      }

      if (error.status === 400) {
        this.submitError.set(
          'Los datos de la transferencia no son válidos.',
        );
        return;
      }

      if (error.status === 403) {
        this.submitError.set(
          'No tienes permisos para realizar esta transferencia.',
        );
        return;
      }
    }

    this.submitError.set(
      'No hemos podido realizar la transferencia. Inténtalo de nuevo.',
    );
  }
}
