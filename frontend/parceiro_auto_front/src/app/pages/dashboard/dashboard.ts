import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CompanyService } from '../companies/company.service';
import { Company } from '../companies/company.model';
import { TransactionService } from '../transactions/transaction.service';
import { Transaction } from '../transactions/transaction.model';

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

  companies = signal<Company[]>([]);
  all = signal<Transaction[]>([]);
  loading = signal(false);

  companyId = signal<number | 'all'>('all');

  today = new Date().toLocaleDateString('pt-BR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });

  /** Current month in YYYY-MM format, used to filter transactions. */
  private currentMonth = new Date().toISOString().slice(0, 7);

  transactions = computed(() => {
    const filter = this.companyId();
    return filter === 'all'
      ? this.all()
      : this.all().filter((m) => m.companyId === filter);
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
      counts.set(m.category, (counts.get(m.category) ?? 0) + m.value);
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
    }).subscribe({
      next: ({ companies, transactions }) => {
        this.companies.set(companies);
        this.all.set(transactions);
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

  formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  date(iso: string): string {
    const [, month, day] = iso.split('-');
    return `${day}/${month}`;
  }
}