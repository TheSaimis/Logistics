import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InventoryService } from '../../core/inventory.service';
import { StockLevel, StocktakeResult, Warehouse } from '../../core/models';

interface CountRow {
  level: StockLevel;
  counted: number;
}

/** Guided stock counting per warehouse — pattern borrowed from Odoo's inventory adjustments. */
@Component({
  selector: 'app-stocktake',
  imports: [CommonModule, FormsModule],
  templateUrl: './stocktake.html',
  styleUrl: './stocktake.scss',
})
export class StocktakePage implements OnInit {
  private api = inject(InventoryService);

  warehouses = signal<Warehouse[]>([]);
  rows = signal<CountRow[]>([]);
  result = signal<StocktakeResult | null>(null);
  error = signal('');
  submitting = signal(false);

  warehouseId: number | null = null;

  ngOnInit(): void {
    this.api.warehouses().subscribe((w) => this.warehouses.set(w));
  }

  loadCounts(): void {
    this.result.set(null);
    this.rows.set([]);
    if (this.warehouseId == null) return;
    this.api.warehouseStock(this.warehouseId).subscribe({
      next: (levels) => this.rows.set(levels.map((level) => ({ level, counted: level.quantity }))),
      error: () => this.error.set('Failed to load stock for this warehouse.'),
    });
  }

  varianceCount(): number {
    return this.rows().filter((r) => r.counted !== r.level.quantity).length;
  }

  submit(): void {
    if (this.warehouseId == null || this.rows().length === 0) return;
    if (!confirm(`Submit stock take? ${this.varianceCount()} item(s) will be adjusted.`)) return;
    this.submitting.set(true);
    this.error.set('');
    const counts = this.rows().map((r) => ({ productId: r.level.productId, counted: r.counted }));
    this.api.stocktake(this.warehouseId, counts).subscribe({
      next: (result) => {
        this.submitting.set(false);
        this.result.set(result);
        this.loadCountsKeepResult();
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err?.error?.message ?? 'Stock take failed.');
      },
    });
  }

  private loadCountsKeepResult(): void {
    if (this.warehouseId == null) return;
    this.api.warehouseStock(this.warehouseId).subscribe((levels) =>
      this.rows.set(levels.map((level) => ({ level, counted: level.quantity }))));
  }
}
