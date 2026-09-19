import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Company } from '../company.model';
import { CompanyService } from '../company.service';
import { TransactionCategory, TransactionType } from '../../transactions/transaction.model';
import { TransactionService } from '../../transactions/transaction.service';

@Component({
  selector: 'app-company-categories',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './company-categories.html',
  styleUrl: './company-categories.scss',
})
export class CompanyCategories implements OnInit {
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private companyService = inject(CompanyService);
  private transactionService = inject(TransactionService);

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

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const companyId = this.companyId();
    this.loading.set(true);

    this.companyService.findById(companyId).subscribe({
      next: (company) => this.company.set(company),
    });

    this.transactionService.listCategoriesByCompany(companyId).subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
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
    const data = this.form.getRawValue();
    const payload = {
      name: data.name?.trim() ?? '',
      type: data.type as TransactionType,
    };
    const editing = this.categoryBeingEdited();

    this.saving.set(true);

    const request = editing
      ? this.transactionService.updateCategory(companyId, editing.id, payload)
      : this.transactionService.createCategory(companyId, payload);

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

    if (!category) {
      return;
    }

    this.transactionService.deleteCategory(category.companyId, category.id).subscribe(() => {
      this.closeConfirmation();
      this.load();
    });
  }

  private companyId(): number {
    return Number(this.route.snapshot.paramMap.get('id'));
  }
}
