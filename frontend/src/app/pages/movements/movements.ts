import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { InventoryService } from '../../core/inventory.service';
import { MovementRequest, Page, Product, StockMovement, Warehouse } from '../../core/models';

@Component({
  selector: 'app-movements',
  imports: [CommonModule, FormsModule],
  templateUrl: './movements.html',
  styleUrl: './movements.scss',
})
export class MovementsPage implements OnInit {
  private api = inject(InventoryService);
  readonly auth = inject(AuthService);

  page = signal<Page<StockMovement> | null>(null);
  products = signal<Product[]>([]);
  warehouses = signal<Warehouse[]>([]);
  error = signal('');
  pageIndex = 0;

  showForm = signal(false);
  form: MovementRequest = this.emptyForm();

  ngOnInit(): void {
    this.api.products({ status: 'ACTIVE' }, 0, 200).subscribe((p) => this.products.set(p.content));
    this.api.warehouses().subscribe((w) => this.warehouses.set(w));
    this.load();
  }

  load(): void {
    this.api.movements(null, this.pageIndex).subscribe({
      next: (page) => this.page.set(page),
      error: () => this.error.set('Failed to load movements.'),
    });
  }

  goToPage(index: number): void {
    this.pageIndex = index;
    this.load();
  }

  openForm(): void {
    this.form = this.emptyForm();
    this.error.set('');
    this.showForm.set(true);
  }

  save(): void {
    this.api.recordMovement(this.form).subscribe({
      next: () => {
        this.showForm.set(false);
        this.load();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Failed to record movement.'),
    });
  }

  private emptyForm(): MovementRequest {
    return { productId: 0, warehouseId: 0, targetWarehouseId: null, type: 'IN', quantity: 1, reference: '', note: '' };
  }
}
