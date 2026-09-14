import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly USERS_KEY = 'parceiro-auto:users:v1';
  private readonly SESSION_KEY = 'parceiro-auto:logado:v1';
  private readonly LATENCY = 300;

  private readonly SEED_USERS: User[] = [
    {
      id: 1,
      name: 'Gustavo Mendes',
      email: 'gustavo@empresa.com',
      password: '123456',
      role: 'admin',
      companyIds: [1, 2],
      active: true,
    },
    {
      id: 2,
      name: 'Maria Silva',
      email: 'maria@empresa.com',
      password: '123456',
      role: 'user',
      companyIds: [1],
      active: true,
    },
    {
      id: 3,
      name: 'João Santos',
      email: 'joao@empresa.com',
      password: '123456',
      role: 'user',
      companyIds: [2, 3],
      active: true,
    },
  ];

  constructor() {
    if (localStorage.getItem(this.USERS_KEY) === null) {
      this.writeUsers(this.SEED_USERS);
    }
  }

  private readUsers(): User[] {
    try {
      const raw = localStorage.getItem(this.USERS_KEY);
      return raw ? (JSON.parse(raw) as User[]) : [];
    } catch {
      this.writeUsers(this.SEED_USERS);
      return [...this.SEED_USERS];
    }
  }

  private writeUsers(users: User[]): void {
    localStorage.setItem(this.USERS_KEY, JSON.stringify(users));
  }

  private readSession(): User | null {
    try {
      const raw = localStorage.getItem(this.SESSION_KEY);
      return raw ? (JSON.parse(raw) as User) : null;
    } catch {
      return null;
    }
  }

  private writeSession(user: User | null): void {
    if (user === null) {
      localStorage.removeItem(this.SESSION_KEY);
    } else {
      localStorage.setItem(this.SESSION_KEY, JSON.stringify(user));
    }
  }

  login(email: string, password: string): Observable<User> {
    const user = this.readUsers().find(
      (u) => u.email === email && u.password === password && u.active
    );

    if (!user) {
      return throwError(() => new Error('Email ou senha inválidos'));
    }

    this.writeSession(user);

    return of(user).pipe(delay(this.LATENCY));
  }

  logout(): void {
    this.writeSession(null);
  }

  isAuthenticated(): boolean {
    return this.readSession() !== null;
  }

  getCurrentUser(): User | null {
    return this.readSession();
  }

  register(data: Omit<User, 'id' | 'companyIds'>): Observable<User> {
    const users = this.readUsers();
    
    // Check whether the email is already registered
    if (users.some((u) => u.email === data.email)) {
      return throwError(() => new Error('Email já cadastrado'));
    }

    const newUser: User = {
      ...data,
      id: users.length > 0 ? Math.max(...users.map((u) => u.id)) + 1 : 1,
      companyIds: [],
    };

    users.push(newUser);
    this.writeUsers(users);
    this.writeSession(newUser);

    return of(newUser).pipe(delay(this.LATENCY));
  }
}
