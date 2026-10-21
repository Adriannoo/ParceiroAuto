import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, Router, UrlTree } from '@angular/router';
import { Observable, map } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate, CanActivateChild {
  constructor(private authService: AuthService, private router: Router) {}

  canActivateChild(): Observable<boolean | UrlTree> { return this.canActivate(); }

  canActivate(): Observable<boolean | UrlTree> {
    return this.authService.checkSession().pipe(
      map(authenticated => authenticated ? true : this.router.createUrlTree(['/login'])),
    );
  }
}
