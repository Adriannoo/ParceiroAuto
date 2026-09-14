import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CompanyService } from '../company.service';
import {
  Company,
  formatAddress,
  legalNatureLabel,
  companySizeLabel,
  taxRegimeLabel,
} from '../company.model';
import { TransactionService } from '../../transactions/transaction.service';
import { Transaction, paymentMethodLabel } from '../../transactions/transaction.model';

@Component({
  selector: 'app-company-transactions',
  imports: [RouterLink],
  templateUrl: './company-transactions.html',
  styleUrl: './company-transactions.scss',
})
export class CompanyTransactions implements OnInit {
  private companyService = inject(CompanyService);
  private transactionService = inject(TransactionService);
  private route = inject(ActivatedRoute);

  company = signal<Company | null>(null);
  transactions = signal<Transaction[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  sorted = computed(() =>
    [...this.transactions()].sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id),
  );

  income = computed(() =>
    this.transactions()
      .filter((m) => m.type === 'ENTRADA')
      .reduce((sum, m) => sum + m.value, 0),
  );

  expenses = computed(() =>
    this.transactions()
      .filter((m) => m.type === 'SAIDA')
      .reduce((sum, m) => sum + m.value, 0),
  );

  balance = computed(() => this.income() - this.expenses());

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading.set(true);

    forkJoin({
      company: this.companyService.findById(id),
      transactions: this.transactionService.list(),
    }).subscribe({
      next: ({ company, transactions }) => {
        this.company.set(company);
        this.transactions.set(transactions.filter((m) => m.companyId === id));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Empresa não encontrada.');
        this.loading.set(false);
      },
    });
  }

  address(e: Company): string {
    return formatAddress(e);
  }

  taxRegime(e: Company): string {
    return taxRegimeLabel(e.taxRegime);
  }

  size(e: Company): string {
    return companySizeLabel(e.size);
  }

  legalNatureLabel(e: Company): string {
    return legalNatureLabel(e.legalNature);
  }

  method(m: Transaction): string {
    return paymentMethodLabel(m.method);
  }

  formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  date(iso: string): string {
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }
}