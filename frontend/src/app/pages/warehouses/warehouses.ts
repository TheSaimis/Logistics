import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { InventoryService } from '../../core/inventory.service';
import { StockLevel, Warehouse } from '../../core/models';

@Component({
  selector: 'app-warehouses',
  imports: [CommonModule, FormsModule],
  templateUrl: './warehouses.html',
  styleUrl: './warehouses.scss',
})
export class WarehousesPage implements OnInit {
  private api = inject(InventoryService);
  readonly auth = inject(AuthService);

  warehouses = signal<Warehouse[]>([]);
  error = signal('');

  showForm = signal(false);
  editing = signal<Warehouse | null>(null);
  form: Partial<Warehouse> = {};

  detail = signal<Warehouse | null>(null);
  detailStock = signal<StockLevel[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.warehouses().subscribe({
      next: (w) => this.warehouses.set(w),
      error: () => this.error.set('Failed to load warehouses.'),
    });
  }

  openCreate(): void {
    this.editing.set(null);
    this.form = {};
    this.error.set('');
    this.showForm.set(true);
  }

  openEdit(warehouse: Warehouse): void {
    this.editing.set(warehouse);
    this.form = { ...warehouse };
    this.error.set('');
    this.showForm.set(true);
  }

  openDetail(warehouse: Warehouse): void {
    this.detail.set(warehouse);
    this.detailStock.set([]);
    this.savedLevelId.set(null);
    this.api.warehouseStock(warehouse.id).subscribe((s) => this.detailStock.set(s));
  }

  savedLevelId = signal<number | null>(null);

  saveLevelSettings(level: StockLevel): void {
    this.api.updateLevelSettings(level.id, {
      bin: level.bin,
      minQuantity: level.minQuantity ?? null,
      maxQuantity: level.maxQuantity ?? null,
    }).subscribe({
      next: () => {
        this.savedLevelId.set(level.id);
        setTimeout(() => this.savedLevelId.set(null), 1500);
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Failed to save stock settings.'),
    });
  }

  save(): void {
    const editing = this.editing();
    const request = editing
      ? this.api.updateWarehouse(editing.id, this.form)
      : this.api.createWarehouse(this.form);
    request.subscribe({
      next: () => {
        this.showForm.set(false);
        this.load();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Save failed.'),
    });
  }

  remove(warehouse: Warehouse): void {
    if (!confirm(`Delete warehouse ${warehouse.code}? This fails if it still holds stock history.`)) return;
    this.api.deleteWarehouse(warehouse.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(err?.error?.message ?? 'Delete failed — warehouse may still be referenced.'),
    });
  }
}
