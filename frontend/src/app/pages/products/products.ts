import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { InventoryService } from '../../core/inventory.service';
import { Category, ImportResult, Page, Product, ProductRequest, StockLevel, Supplier } from '../../core/models';

interface ColumnDef {
  key: string;
  label: string;
  sortField?: string; // backend entity property; absent = not sortable
  locked?: boolean; // always shown, cannot be toggled off
}

const COLUMNS: ColumnDef[] = [
  { key: 'sku', label: 'SKU', sortField: 'sku', locked: true },
  { key: 'name', label: 'Name', sortField: 'name', locked: true },
  { key: 'category', label: 'Category' },
  { key: 'supplier', label: 'Supplier' },
  { key: 'unitPrice', label: 'Unit price', sortField: 'unitPrice' },
  { key: 'reorderLevel', label: 'Reorder level', sortField: 'reorderLevel' },
  { key: 'totalQuantity', label: 'In stock' },
  { key: 'createdAt', label: 'Created', sortField: 'createdAt' },
  { key: 'status', label: 'Status' },
];

const LOCKED_COLS = COLUMNS.filter((c) => c.locked).map((c) => c.key);

const COLS_STORAGE_KEY = 'lg_product_cols';
const PAGESIZE_STORAGE_KEY = 'lg_product_pagesize';

@Component({
  selector: 'app-products',
  imports: [CommonModule, FormsModule],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class ProductsPage implements OnInit {
  private api = inject(InventoryService);
  readonly auth = inject(AuthService);

  readonly columns = COLUMNS;
  readonly pageSizes = [10, 20, 50, 100];

  page = signal<Page<Product> | null>(null);
  categories = signal<Category[]>([]);
  suppliers = signal<Supplier[]>([]);
  loading = signal(false);
  error = signal('');

  // filters & table settings
  search = '';
  categoryFilter: number | null = null;
  supplierFilter: number | null = null;
  statusFilter: 'ALL' | 'ACTIVE' | 'INACTIVE' = 'ALL';
  stockFilter: 'ALL' | 'IN' | 'LOW' | 'OUT' = 'ALL';
  sortField = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';
  pageIndex = 0;
  pageSize = this.restorePageSize();

  visibleCols = signal<Set<string>>(this.restoreColumns());
  showColumnMenu = signal(false);

  // form modal state
  showForm = signal(false);
  editing = signal<Product | null>(null);
  form: ProductRequest = this.emptyForm();

  // detail modal state
  detail = signal<Product | null>(null);
  detailStock = signal<StockLevel[]>([]);

  // excel import/export state
  importing = signal(false);
  importResult = signal<ImportResult | null>(null);

  ngOnInit(): void {
    this.api.categories().subscribe((c) => this.categories.set(c));
    this.api.suppliers().subscribe((s) => this.suppliers.set(s));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api
      .products(
        {
          search: this.search,
          categoryId: this.categoryFilter,
          supplierId: this.supplierFilter,
          status: this.statusFilter,
          stockStatus: this.stockFilter,
          sort: this.sortField,
          direction: this.sortDirection,
        },
        this.pageIndex,
        this.pageSize
      )
      .subscribe({
        next: (page) => {
          this.page.set(page);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Failed to load products.');
          this.loading.set(false);
        },
      });
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.load();
  }

  resetFilters(): void {
    this.search = '';
    this.categoryFilter = null;
    this.supplierFilter = null;
    this.statusFilter = 'ALL';
    this.stockFilter = 'ALL';
    this.applyFilters();
  }

  sortBy(col: ColumnDef): void {
    if (!col.sortField) return;
    if (this.sortField === col.sortField) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = col.sortField;
      this.sortDirection = 'asc';
    }
    this.pageIndex = 0;
    this.load();
  }

  sortIndicator(col: ColumnDef): string {
    if (!col.sortField || this.sortField !== col.sortField) return '';
    return this.sortDirection === 'asc' ? ' ▲' : ' ▼';
  }

  changePageSize(): void {
    localStorage.setItem(PAGESIZE_STORAGE_KEY, String(this.pageSize));
    this.pageIndex = 0;
    this.load();
  }

  isVisible(key: string): boolean {
    return this.visibleCols().has(key);
  }

  isLocked(key: string): boolean {
    return LOCKED_COLS.includes(key);
  }

  toggleColumn(key: string): void {
    if (this.isLocked(key)) return; // SKU + Name are always shown
    const next = new Set(this.visibleCols());
    if (next.has(key)) {
      next.delete(key);
    } else {
      next.add(key);
    }
    this.visibleCols.set(next);
    localStorage.setItem(COLS_STORAGE_KEY, JSON.stringify([...next]));
  }

  visibleCount(): number {
    return this.visibleCols().size + 1; // + actions column
  }

  goToPage(index: number): void {
    this.pageIndex = index;
    this.load();
  }

  openCreate(): void {
    this.editing.set(null);
    this.form = this.emptyForm();
    this.error.set('');
    this.showForm.set(true);
  }

  openEdit(product: Product): void {
    this.editing.set(product);
    this.form = {
      sku: product.sku,
      name: product.name,
      description: product.description,
      categoryId: product.categoryId ?? null,
      supplierId: product.supplierId ?? null,
      unitPrice: product.unitPrice,
      reorderLevel: product.reorderLevel,
      active: product.active,
    };
    this.error.set('');
    this.showForm.set(true);
  }

  openDetail(product: Product): void {
    this.detail.set(product);
    this.detailStock.set([]);
    this.api.stockForProduct(product.id).subscribe((levels) => this.detailStock.set(levels));
  }

  save(): void {
    const editing = this.editing();
    const request = editing
      ? this.api.updateProduct(editing.id, this.form)
      : this.api.createProduct(this.form);
    request.subscribe({
      next: () => {
        this.showForm.set(false);
        this.load();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Save failed.'),
    });
  }

  deactivate(product: Product): void {
    if (!confirm(`Deactivate "${product.name}"? Movement history is kept.`)) return;
    this.api.deleteProduct(product.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(err?.error?.message ?? 'Delete failed.'),
    });
  }

  exportExcel(): void {
    this.api.exportProductsExcel().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `inventory-${new Date().toISOString().slice(0, 10)}.xlsx`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.error.set('Export failed.'),
    });
  }

  onImportFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    input.value = ''; // allow re-selecting the same file
    this.importing.set(true);
    this.importResult.set(null);
    this.error.set('');
    this.api.importProductsExcel(file).subscribe({
      next: (result) => {
        this.importing.set(false);
        this.importResult.set(result);
        this.load();
      },
      error: (err) => {
        this.importing.set(false);
        this.error.set(err?.error?.message ?? 'Import failed.');
      },
    });
  }

  private emptyForm(): ProductRequest {
    return { sku: '', name: '', description: '', categoryId: null, supplierId: null, unitPrice: 0, reorderLevel: 0, active: true };
  }

  private restoreColumns(): Set<string> {
    try {
      const raw = localStorage.getItem(COLS_STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as string[];
        if (Array.isArray(parsed) && parsed.length > 0) {
          // Locked columns are always present regardless of stored preferences
          return new Set([...LOCKED_COLS, ...parsed]);
        }
      }
    } catch { /* fall through to defaults */ }
    return new Set(['sku', 'name', 'category', 'supplier', 'unitPrice', 'totalQuantity', 'status']);
  }

  private restorePageSize(): number {
    const stored = Number(localStorage.getItem(PAGESIZE_STORAGE_KEY));
    return this.pageSizes?.includes(stored) ? stored : 20;
  }
}
