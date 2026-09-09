import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
  ReactiveFormsModule,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  form: FormGroup;
  formSubmitted = signal(false);
  carregando = signal(false);
  erro = signal<string | null>(null);

  constructor() {
    this.form = this.fb.group(
      {
        nome: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', [Validators.required]]
      },
      {
        validators: this.passwordsMatch
      }
    );
  }

  get nomeControl() {
    return this.form.get('nome');
  }

  get emailControl() {
    return this.form.get('email');
  }

  get passwordControl() {
    return this.form.get('password');
  }

  get confirmPasswordControl() {
    return this.form.get('confirmPassword');
  }

  passwordsMatch(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;

    if (!password || !confirmPassword) {
      return null;
    }

    return password === confirmPassword ? null : { passwordsNotMatch: true };
  }

  submit() {
    this.erro.set(null);
    this.formSubmitted.set(true);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.carregando.set(true);

    const { nome, email, password } = this.form.value;

    this.authService.registrar({
      nome,
      email,
      senha: password,
      papel: 'dono',
      ativo: true,
    }).subscribe({
      next: () => {
        this.carregando.set(false);
        // Redireciona para dashboard (já logado)
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.carregando.set(false);
        this.erro.set(err.message);
      },
    });
  }

  limparErro(): void {
    this.erro.set(null);
  }
}