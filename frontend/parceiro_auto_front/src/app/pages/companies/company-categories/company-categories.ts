import { Component, computed, effect, untracked, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
import { CurrentCompanyService } from '../../../services/current-company.service';
import { Company } from '../company.model';
import { CompanyService } from '../company.service';
import { TransactionCategory, TransactionType } from '../../transactions/transaction.model';
import { TransactionCategoryService } from '../../transactions/transaction-category.service';

@Component({
  selector: 'app-company-categories',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './company-categories.html',
  styleUrl: './company-categories.scss',
})
export class CompanyCategories {
  private router = inject(Router);
  private currentCompanyService = inject(CurrentCompanyService);
  companyId = this.currentCompanyService.currentCompanyId;
  private dataSubscription?: Subscription;
  private fb = inject(FormBuilder);
  private companyService = inject(CompanyService);
  private categoryService = inject(TransactionCategoryService);

  company = signal<Company | null>(null);
  categories = signal<TransactionCategory[]>([]);
  loading = signal(false);
  saving = signal(false);
  categoryToDelete = signal<TransactionCategory | null>(null);
  categoryBeingEdited = signal<TransactionCategory | null>(null);
  categoryModalOpen = signal(false);
  error = signal<string | null>(null);

  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    type: ['ENTRADA' as TransactionType, [Validators.required]],
  });

  incomeCategories = computed(() => this.categories().filter((category) => category.type === 'ENTRADA'));
  expenseCategories = computed(() => this.categories().filter((category) => category.type === 'SAIDA'));

  constructor() {
    effect(onCleanup => {
      const companyId = this.companyId();
      untracked(() => {
        this.categoryToDelete.set(null);
        this.categoryBeingEdited.set(null);
        this.categoryModalOpen.set(false);
        if (companyId) void this.router.navigate(['/companies', companyId, 'categories'], { replaceUrl: true });
        this.load();
      });
      onCleanup(() => this.dataSubscription?.unsubscribe());
    });
  }

  load(): void {
    this.dataSubscription?.unsubscribe();
    const companyId = this.companyId();
    this.company.set(null);
    this.categories.set([]);
    this.loading.set(false);
    if (!companyId) {
      this.error.set('Selecione uma empresa na sidebar ou cadastre uma empresa para gerenciar categorias.');
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    this.dataSubscription = forkJoin({
      company: this.companyService.findById(companyId),
      categories: this.categoryService.listCategoriesByCompany(companyId),
    }).subscribe({
      next: ({ company, categories }) => {
        if (this.companyId() !== companyId) return;
        this.company.set(company);
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Nao foi possivel carregar as categorias da empresa atual.');
      },
    });
  }

  openCreateModal(type: TransactionType): void {
    this.categoryBeingEdited.set(null);
    this.error.set(null);
    this.form.reset({ name: '', type });
    this.categoryModalOpen.set(true);
  }

  openEditModal(category: TransactionCategory): void {
    this.categoryBeingEdited.set(category);
    this.error.set(null);
    this.form.reset({ name: category.name, type: category.type });
    this.categoryModalOpen.set(true);
  }

  closeCategoryModal(): void {
    if (this.saving()) {
      return;
    }

    this.categoryModalOpen.set(false);
    this.categoryBeingEdited.set(null);
    this.error.set(null);
    this.form.reset({ name: '', type: 'ENTRADA' });
  }

  saveCategory(): void {
    this.error.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const companyId = this.companyId();
    if (!companyId || this.saving() || this.loading() || this.company()?.id !== companyId) {
      this.error.set('Selecione uma empresa na sidebar e aguarde o carregamento.');
      return;
    }
    const data = this.form.getRawValue();
    const payload = {
      name: data.name?.trim() ?? '',
      type: data.type as TransactionType,
    };
    const editing = this.categoryBeingEdited();

    if (editing && editing.companyId !== companyId) return;
    this.saving.set(true);

    const request = editing
      ? this.categoryService.updateCategory(companyId, editing.id, payload)
      : this.categoryService.createCategory(companyId, payload);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeCategoryModal();
        this.load();
      },
      error: () => {
        this.error.set('Nao foi possivel salvar a categoria. Verifique o nome e tente novamente.');
        this.saving.set(false);
      },
    });
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  openConfirmation(category: TransactionCategory): void {
    this.categoryToDelete.set(category);
  }

  closeConfirmation(): void {
    this.categoryToDelete.set(null);
  }

  confirmDeletion(): void {
    const category = this.categoryToDelete();

    if (!category || category.companyId !== this.companyId()) {
      return;
    }

    this.categoryService.deleteCategory(category.companyId, category.id).subscribe({
      next: () => {
        this.closeConfirmation();
        this.load();
      },
      error: () => this.error.set('Nao foi possivel inativar a categoria. Tente novamente.'),
    });
  }

}
