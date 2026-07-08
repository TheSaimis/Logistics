import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from './api';
import {
  AnalyticsResponse,
  AuditEntry,
  Category,
  DashboardStats,
  MovementRequest,
  Page,
  Product,
  ProductFilters,
  ProductRequest,
  StockLevel,
  StockMovement,
  Supplier,
  UserDto,
  Warehouse,
} from './models';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private http = inject(HttpClient);

  // Dashboard
  dashboard(warehouseId: number | null = null): Observable<DashboardStats> {
    let params = new HttpParams();
    if (warehouseId != null) params = params.set('warehouseId', warehouseId);
    return this.http.get<DashboardStats>(`${API_URL}/dashboard`, { params });
  }

  // Admin analytics
  analytics(): Observable<AnalyticsResponse> {
    return this.http.get<AnalyticsResponse>(`${API_URL}/admin/analytics`);
  }

  // Admin audit trail
  auditLog(page: number, size = 25): Observable<Page<AuditEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<AuditEntry>>(`${API_URL}/admin/audit`, { params });
  }

  // Products
  products(filters: Partial<ProductFilters>, page: number, size = 20): Observable<Page<Product>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.search) params = params.set('search', filters.search);
    if (filters.categoryId != null) params = params.set('categoryId', filters.categoryId);
    if (filters.supplierId != null) params = params.set('supplierId', filters.supplierId);
    if (filters.status && filters.status !== 'ALL') params = params.set('status', filters.status);
    if (filters.stockStatus && filters.stockStatus !== 'ALL') params = params.set('stockStatus', filters.stockStatus);
    if (filters.sort) params = params.set('sort', `${filters.sort},${filters.direction ?? 'asc'}`);
    return this.http.get<Page<Product>>(`${API_URL}/products`, { params });
  }

  product(id: number): Observable<Product> {
    return this.http.get<Product>(`${API_URL}/products/${id}`);
  }

  createProduct(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(`${API_URL}/products`, request);
  }

  updateProduct(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${API_URL}/products/${id}`, request);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/products/${id}`);
  }

  // Categories
  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${API_URL}/categories`);
  }

  createCategory(body: Partial<Category>): Observable<Category> {
    return this.http.post<Category>(`${API_URL}/categories`, body);
  }

  updateCategory(id: number, body: Partial<Category>): Observable<Category> {
    return this.http.put<Category>(`${API_URL}/categories/${id}`, body);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/categories/${id}`);
  }

  // Suppliers
  suppliers(): Observable<Supplier[]> {
    return this.http.get<Supplier[]>(`${API_URL}/suppliers`);
  }

  createSupplier(body: Partial<Supplier>): Observable<Supplier> {
    return this.http.post<Supplier>(`${API_URL}/suppliers`, body);
  }

  updateSupplier(id: number, body: Partial<Supplier>): Observable<Supplier> {
    return this.http.put<Supplier>(`${API_URL}/suppliers/${id}`, body);
  }

  deleteSupplier(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/suppliers/${id}`);
  }

  // Warehouses
  warehouses(): Observable<Warehouse[]> {
    return this.http.get<Warehouse[]>(`${API_URL}/warehouses`);
  }

  warehouseStock(id: number): Observable<StockLevel[]> {
    return this.http.get<StockLevel[]>(`${API_URL}/warehouses/${id}/stock`);
  }

  createWarehouse(body: Partial<Warehouse>): Observable<Warehouse> {
    return this.http.post<Warehouse>(`${API_URL}/warehouses`, body);
  }

  updateWarehouse(id: number, body: Partial<Warehouse>): Observable<Warehouse> {
    return this.http.put<Warehouse>(`${API_URL}/warehouses/${id}`, body);
  }

  deleteWarehouse(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/warehouses/${id}`);
  }

  // Stock
  stockForProduct(productId: number): Observable<StockLevel[]> {
    return this.http.get<StockLevel[]>(`${API_URL}/stock/product/${productId}`);
  }

  lowStock(): Observable<StockLevel[]> {
    return this.http.get<StockLevel[]>(`${API_URL}/stock/low`);
  }

  movements(productId: number | null, page: number, size = 20): Observable<Page<StockMovement>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (productId != null) params = params.set('productId', productId);
    return this.http.get<Page<StockMovement>>(`${API_URL}/stock/movements`, { params });
  }

  recordMovement(request: MovementRequest): Observable<StockMovement> {
    return this.http.post<StockMovement>(`${API_URL}/stock/movements`, request);
  }

  // Admin users
  users(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${API_URL}/admin/users`);
  }

  createUser(body: object): Observable<UserDto> {
    return this.http.post<UserDto>(`${API_URL}/admin/users`, body);
  }

  updateUser(id: number, body: object): Observable<UserDto> {
    return this.http.put<UserDto>(`${API_URL}/admin/users/${id}`, body);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/users/${id}`);
  }
}
