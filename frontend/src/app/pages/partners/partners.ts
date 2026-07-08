import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { InventoryService } from '../../core/inventory.service';
import { Category, Supplier } from '../../core/models';

@Component({
  selector: 'app-partners',
  imports: [CommonModule, FormsModule],
  templateUrl: './partners.html',
  styleUrl: './partners.scss',
})
export class PartnersPage implements OnInit {
  private api = inject(InventoryService);
  readonly auth = inject(AuthService);

  categories = signal<Category[]>([]);
  suppliers = signal<Supplier[]>([]);
  error = signal('');

  editingCategory = signal<Category | null>(null);
  showCategoryForm = signal(false);
  categoryForm: Partial<Category> = {};

  editingSupplier = signal<Supplier | null>(null);
  showSupplierForm = signal(false);
  supplierForm: Partial<Supplier> = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.categories().subscribe((c) => this.categories.set(c));
    this.api.suppliers().subscribe((s) => this.suppliers.set(s));
  }

  openCategory(category: Category | null): void {
    this.editingCategory.set(category);
    this.categoryForm = category ? { ...category } : {};
    this.error.set('');
    this.showCategoryForm.set(true);
  }

  saveCategory(): void {
    const editing = this.editingCategory();
    const request = editing
      ? this.api.updateCategory(editing.id, this.categoryForm)
      : this.api.createCategory(this.categoryForm);
    request.subscribe({
      next: () => {
        this.showCategoryForm.set(false);
        this.load();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Save failed.'),
    });
  }

  deleteCategory(category: Category): void {
    if (!confirm(`Delete category "${category.name}"?`)) return;
    this.api.deleteCategory(category.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Delete failed — category may be in use by products.'),
    });
  }

  openSupplier(supplier: Supplier | null): void {
    this.editingSupplier.set(supplier);
    this.supplierForm = supplier ? { ...supplier } : {};
    this.error.set('');
    this.showSupplierForm.set(true);
  }

  saveSupplier(): void {
    const editing = this.editingSupplier();
    const request = editing
      ? this.api.updateSupplier(editing.id, this.supplierForm)
      : this.api.createSupplier(this.supplierForm);
    request.subscribe({
      next: () => {
        this.showSupplierForm.set(false);
        this.load();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Save failed.'),
    });
  }

  deleteSupplier(supplier: Supplier): void {
    if (!confirm(`Delete supplier "${supplier.name}"?`)) return;
    this.api.deleteSupplier(supplier.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Delete failed — supplier may be in use by products.'),
    });
  }
}
