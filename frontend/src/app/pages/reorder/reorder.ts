import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { InventoryService } from '../../core/inventory.service';
import { ReorderSuggestion } from '../../core/models';

interface SupplierGroup {
  supplierName: string;
  suggestions: ReorderSuggestion[];
}

/** Suggested purchases from reorder rules — pattern borrowed from Odoo's reordering rules. */
@Component({
  selector: 'app-reorder',
  imports: [CommonModule],
  templateUrl: './reorder.html',
  styleUrl: './reorder.scss',
})
export class ReorderPage implements OnInit {
  private api = inject(InventoryService);

  suggestions = signal<ReorderSuggestion[]>([]);
  error = signal('');
  loaded = signal(false);

  groups = computed<SupplierGroup[]>(() => {
    const map = new Map<string, ReorderSuggestion[]>();
    for (const s of this.suggestions()) {
      const key = s.supplierName ?? 'No supplier assigned';
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(s);
    }
    return [...map.entries()].map(([supplierName, suggestions]) => ({ supplierName, suggestions }));
  });

  ngOnInit(): void {
    this.api.reorderSuggestions().subscribe({
      next: (s) => {
        this.suggestions.set(s);
        this.loaded.set(true);
      },
      error: () => this.error.set('Failed to load reorder suggestions.'),
    });
  }
}
