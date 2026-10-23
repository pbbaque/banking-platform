import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';

import {
  CustomerResponse,
  CustomerService,
  UpdateCustomerContactRequest,
} from '../../../core/api/customer';

@Component({
  selector: 'app-customer-profile',
  imports: [
    DatePipe,
    ReactiveFormsModule,
  ],
  templateUrl: './customer-profile.html',
  styleUrl: './customer-profile.scss',
})
export class CustomerProfile {
  private readonly customerService =
    inject(CustomerService);

  private readonly formBuilder =
    inject(FormBuilder);

  readonly customer =
    signal<CustomerResponse | null>(null);

  readonly loading = signal(true);
  readonly editing = signal(false);
  readonly saving = signal(false);

  readonly loadError =
    signal<string | null>(null);

  readonly saveError =
    signal<string | null>(null);

  readonly saveSuccess = signal(false);

  readonly initials = computed(() => {
    const customer = this.customer();

    if (!customer) {
      return '';
    }

    return (
      customer.firstName.charAt(0) +
      customer.lastName.charAt(0)
    ).toUpperCase();
  });

  readonly fullName = computed(() => {
    const customer = this.customer();

    if (!customer) {
      return '';
    }

    return `${customer.firstName} ${customer.lastName}`;
  });

  readonly form = this.formBuilder.group({
    phoneNumber: [
      '',
      [
        Validators.required,
        Validators.pattern(/^[0-9+\s()-]{7,20}$/),
      ],
    ],
    fiscalAddress: [
      '',
      [
        Validators.required,
        Validators.maxLength(200),
      ],
    ],
    postalCode: [
      '',
      [
        Validators.required,
        Validators.pattern(/^[0-9A-Za-z -]{3,12}$/),
      ],
    ],
    city: [
      '',
      [
        Validators.required,
        Validators.maxLength(100),
      ],
    ],
    countryCode: [
      '',
      [
        Validators.required,
        Validators.pattern(/^[A-Za-z]{2}$/),
      ],
    ],
  });

  constructor() {
    this.loadCustomer();
  }

  loadCustomer(): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.customerService
      .getCurrentCustomer()
      .pipe(
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (customer) => {
          this.customer.set(customer);
          this.populateForm(customer);
        },
        error: () => {
          this.loadError.set(
            'No hemos podido cargar los datos de tu perfil.',
          );
        },
      });
  }

  startEditing(): void {
    const customer = this.customer();

    if (!customer) {
      return;
    }

    this.populateForm(customer);
    this.saveError.set(null);
    this.saveSuccess.set(false);
    this.editing.set(true);
  }

  cancelEditing(): void {
    const customer = this.customer();

    if (customer) {
      this.populateForm(customer);
    }

    this.saveError.set(null);
    this.saveSuccess.set(false);
    this.editing.set(false);
  }

  save(): void {
    const customer = this.customer();

    if (!customer) {
      return;
    }

    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    this.saveError.set(null);
    this.saveSuccess.set(false);
    this.saving.set(true);

    const request: UpdateCustomerContactRequest = {
      phoneNumber:
        this.form.controls.phoneNumber.value!.trim(),

      fiscalAddress:
        this.form.controls.fiscalAddress.value!.trim(),

      postalCode:
        this.form.controls.postalCode.value!.trim(),

      city:
        this.form.controls.city.value!.trim(),

      countryCode:
        this.form.controls.countryCode.value!
          .trim()
          .toUpperCase(),
    };

    this.customerService
      .updateContactData(customer.id, request)
      .pipe(
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (updatedCustomer) => {
          this.customer.set(updatedCustomer);
          this.populateForm(updatedCustomer);
          this.editing.set(false);
          this.saveSuccess.set(true);
        },
        error: (error: unknown) => {
          this.handleSaveError(error);
        },
      });
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'Activo';

      case 'BLOCKED':
        return 'Bloqueado';

      case 'INACTIVE':
        return 'Inactivo';

      case 'PENDING_VERIFICATION':
        return 'Pendiente de verificación';

      default:
        return this.formatEnum(status);
    }
  }

  documentTypeLabel(type: string): string {
    switch (type) {
      case 'DNI':
        return 'DNI';

      case 'NIE':
        return 'NIE';

      case 'PASSPORT':
        return 'Pasaporte';

      default:
        return this.formatEnum(type);
    }
  }

  countryLabel(countryCode: string): string {
    switch (countryCode.toUpperCase()) {
      case 'ES':
        return 'España';

      case 'FR':
        return 'Francia';

      case 'PT':
        return 'Portugal';

      case 'DE':
        return 'Alemania';

      case 'IT':
        return 'Italia';

      default:
        return countryCode.toUpperCase();
    }
  }

  private populateForm(
    customer: CustomerResponse,
  ): void {
    this.form.setValue({
      phoneNumber: customer.phoneNumber ?? '',
      fiscalAddress:
        customer.fiscalAddress ?? '',
      postalCode: customer.postalCode ?? '',
      city: customer.city ?? '',
      countryCode:
        customer.countryCode ?? '',
    });
  }

  private handleSaveError(
    error: unknown,
  ): void {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        this.saveError.set(
          'Revisa los datos introducidos. Alguno de los campos no es válido.',
        );
        return;
      }

      if (error.status === 404) {
        this.saveError.set(
          'No hemos podido localizar tu perfil de cliente.',
        );
        return;
      }

      if (error.status === 409) {
        this.saveError.set(
          'No se han podido guardar los cambios por un conflicto con el estado actual del cliente.',
        );
        return;
      }
    }

    this.saveError.set(
      'No hemos podido guardar los cambios. Inténtalo de nuevo.',
    );
  }

  private formatEnum(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map(
        (word) =>
          word.charAt(0).toUpperCase() +
          word.slice(1),
      )
      .join(' ');
  }
}