import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { InventoryService } from '../../core/inventory.service';
import { DashboardStats, Warehouse } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardPage implements OnInit {
  private api = inject(InventoryService);

  stats = signal<DashboardStats | null>(null);
  warehouses = signal<Warehouse[]>([]);
  error = signal('');

  warehouseFilter: number | null = null;

  ngOnInit(): void {
    this.api.warehouses().subscribe((w) => this.warehouses.set(w));
    this.load();
  }

  load(): void {
    this.api.dashboard(this.warehouseFilter).subscribe({
      next: (stats) => this.stats.set(stats),
      error: () => this.error.set('Failed to load dashboard data.'),
    });
  }
}
