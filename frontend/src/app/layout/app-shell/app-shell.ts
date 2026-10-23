import {
  Component,
  ElementRef,
  HostListener,
  inject,
  ViewChild,
} from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';

import { Auth } from '../../core/auth/auth';

@Component({
  selector: 'app-app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
  ],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  @ViewChild('profileMenu')
  private profileMenu?: ElementRef<HTMLDetailsElement>;

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const menu = this.profileMenu?.nativeElement;

    if (!menu?.open) {
      return;
    }

    const target = event.target;

    if (target instanceof Node && !menu.contains(target)) {
      menu.open = false;
    }
  }

  closeProfileMenu(): void {
    const menu = this.profileMenu?.nativeElement;

    if (menu) {
      menu.open = false;
    }
  }

  logout(): void {
    this.closeProfileMenu();
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}