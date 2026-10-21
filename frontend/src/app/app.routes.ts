import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth-guard';
import { AccountDetail } from './features/accounts/account-detail/account-detail';
import { AccountList } from './features/accounts/account-list/account-list';
import { AuditList } from './features/audit/audit-list/audit-list';
import { Login } from './features/auth/login/login';
import { CustomerProfile } from './features/customer/customer-profile/customer-profile';
import { Dashboard } from './features/dashboard/dashboard/dashboard';
import { TransferCreate } from './features/transfers/transfer-create/transfer-create';
import { TransferDetail } from './features/transfers/transfer-detail/transfer-detail';
import { TransferList } from './features/transfers/transfer-list/transfer-list';
import { AppShell } from './layout/app-shell/app-shell';

export const routes: Routes = [
    {
        path: 'login',
        component: Login,
    },
    {
        path: '',
        component: AppShell,
        canActivate: [authGuard],
        children: [
            {
                path: 'dashboard',
                component: Dashboard,
            },
            {
                path: 'accounts',
                component: AccountList,
            },
            {
                path: 'accounts/:id',
                component: AccountDetail,
            },
            {
                path: 'transfers',
                component: TransferList,
            },
            {
                path: 'transfers/new',
                component: TransferCreate,
            },
            {
                path: 'transfers/:id',
                component: TransferDetail,
            },
            {
                path: 'profile',
                component: CustomerProfile,
            },
            {
                path: 'audit',
                component: AuditList,
            },
            {
                path: '',
                pathMatch: 'full',
                redirectTo: 'dashboard',
            },
        ],
    },
    {
        path: '**',
        redirectTo: '',
    },
];