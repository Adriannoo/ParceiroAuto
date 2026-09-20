import { Component, OnInit, effect, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { CompanyService } from '../companies/company.service';
import { Company } from '../companies/company.model';
import { TransactionService } from '../transactions/transaction.service';
import { RecurrenceRuleService } from '../transactions/recurrence-rule.service';
import { TransactionCategoryService } from '../transactions/transaction-category.service';
import { RecurringTransaction, Transaction, TransactionCategory } from '../transactions/transaction.model';
import { CurrentCompanyService } from '../../services/current-company.service';

interface MonthlyBar {
  label: string;
  income: number;
  expenses: number;
  incomeHeight: number;
  expenseHeight: number;
}

interface CategorySlice {
  label: string;
  total: number;
  percentage: number;
}

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private companyService = inject(CompanyService);
  private transactionService = inject(TransactionService);
  private recurrenceService = inject(RecurrenceRuleService);
  private categoryService = inject(TransactionCategoryService);
  private currentCompanyService = inject(CurrentCompanyService);

  companies = signal<Company[]>([]);
  all = signal<Transaction[]>([]);
  categories = signal<TransactionCategory[]>([]);
  recurring = signal<RecurringTransaction[]>([]);
  loading = signal(false);

  companyId = this.currentCompanyService.currentCompanyId;

  today = new Date().toLocaleDateString('pt-BR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });

  /** Current month in YYYY-MM format, used to filter transactions. */
  private currentMonth = new Date().toISOString().slice(0, 7);

  constructor() {
    effect(() => {
      this.companyId();
      this.loadRecurring();
    });
  }

  transactions = computed(() => {
    const filter = this.companyId();
    return filter === null ? this.all() : this.all().filter((m) => m.companyId === filter);
  });

  monthlyTransactions = computed(() =>
    this.transactions().filter((m) => m.date.startsWith(this.currentMonth)),
  );

  monthlyIncome = computed(() => this.sumByType(this.monthlyTransactions(), 'ENTRADA'));
  monthlyExpenses = computed(() => this.sumByType(this.monthlyTransactions(), 'SAIDA'));
  monthlyResult = computed(() => this.monthlyIncome() - this.monthlyExpenses());

  accumulatedBalance = computed(
    () => this.sumByType(this.transactions(), 'ENTRADA') - this.sumByType(this.transactions(), 'SAIDA'),
  );

  averageIncome = computed(() => {
    const income = this.monthlyTransactions().filter((m) => m.type === 'ENTRADA');
    return income.length === 0 ? 0 : this.monthlyIncome() / income.length;
  });

  /** Last six months, with bar heights normalized to the maximum value. */
  evolution = computed<MonthlyBar[]>(() => {
    const months: { key: string; label: string }[] = [];
    const reference = new Date();

    for (let i = 5; i >= 0; i--) {
      const d = new Date(reference.getFullYear(), reference.getMonth() - i, 1);
      months.push({
        key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
        label: d.toLocaleDateString('pt-BR', { month: 'short' }).replace('.', ''),
      });
    }

    const raw = months.map(({ key, label }) => {
      const periodTransactions = this.transactions().filter((m) => m.date.startsWith(key));
      return {
        label,
        income: this.sumByType(periodTransactions, 'ENTRADA'),
        expenses: this.sumByType(periodTransactions, 'SAIDA'),
      };
    });

    const maximum = Math.max(...raw.flatMap((b) => [b.income, b.expenses]), 1);

    return raw.map((b) => ({
      ...b,
      incomeHeight: Math.round((b.income / maximum) * 100),
      expenseHeight: Math.round((b.expenses / maximum) * 100),
    }));
  });

  expensesByCategory = computed<CategorySlice[]>(() => {
    const expenses = this.monthlyTransactions().filter((m) => m.type === 'SAIDA');
    const total = expenses.reduce((sum, m) => sum + m.value, 0);
    const counts = new Map<string, number>();

    for (const m of expenses) {
      const category = this.categoryName(m);
      counts.set(category, (counts.get(category) ?? 0) + m.value);
    }

    return [...counts.entries()]
      .map(([label, value]) => ({
        label,
        total: value,
        percentage: total === 0 ? 0 : Math.round((value / total) * 100),
      }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 5);
  });

  recent = computed(() =>
    [...this.transactions()]
      .sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id)
      .slice(0, 5),
  );

  ngOnInit(): void {
    this.loading.set(true);

    forkJoin({
      companies: this.companyService.list(),
      transactions: this.transactionService.list(),
    }).pipe(
      switchMap(({ companies, transactions }) => {
        this.companies.set(companies);
        this.currentCompanyService.ensureCompany(companies.filter((company) => company.active).map((company) => company.id));
        this.all.set(transactions);

        if (companies.length === 0) {
          return of({ categories: [], recurring: [] });
        }

        const currentCompanyId = this.companyId() ?? companies[0].id;

        return forkJoin({
          categories: forkJoin(companies.map((company) => this.categoryService.listCategoriesByCompany(company.id))),
          recurring: this.recurrenceService.listNextRecurringByCompany(currentCompanyId, 3),
        });
      }),
    ).subscribe({
      next: ({ categories, recurring }) => {
        this.categories.set(categories.flat());
        this.recurring.set(recurring);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private sumByType(list: Transaction[], type: 'ENTRADA' | 'SAIDA'): number {
    return list.filter((m) => m.type === type).reduce((sum, m) => sum + m.value, 0);
  }

  companyName(id: number): string {
    return this.companies().find((e) => e.id === id)?.tradeName ?? 'Empresa removida';
  }

  setCurrentCompany(companyId: number | string): void {
    this.currentCompanyService.setCurrentCompanyId(Number(companyId));
    this.loadRecurring();
  }

  private loadRecurring(): void {
    const companyId = this.companyId();

    if (!companyId) {
      this.recurring.set([]);
      return;
    }

    this.recurrenceService.listNextRecurringByCompany(companyId, 3).subscribe({
      next: (recurring) => this.recurring.set(recurring),
      error: () => this.recurring.set([]),
    });
  }

  categoryName(transaction: Transaction): string {
    return this.categories().find((category) => category.id === transaction.transactionCategoryId)?.name ?? 'Categoria removida';
  }

  formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  date(iso: string): string {
    const [, month, day] = iso.split('-');
    return `${day}/${month}`;
  }
}
