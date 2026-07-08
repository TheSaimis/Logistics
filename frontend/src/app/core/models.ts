export interface UserDto {
  id: number;
  email: string;
  fullName: string;
  roles: string[];
  enabled: boolean;
  provider: string;
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  user: UserDto;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
}

export interface Supplier {
  id: number;
  name: string;
  contactEmail?: string;
  phone?: string;
  address?: string;
}

export interface Warehouse {
  id: number;
  code: string;
  name: string;
  location?: string;
  capacity?: number;
  totalUnits?: number;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description?: string;
  categoryId?: number;
  categoryName?: string;
  supplierId?: number;
  supplierName?: string;
  unitPrice: number;
  reorderLevel: number;
  active: boolean;
  totalQuantity: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  sku: string;
  name: string;
  description?: string;
  categoryId?: number | null;
  supplierId?: number | null;
  unitPrice: number;
  reorderLevel: number;
  active?: boolean;
}

export interface StockLevel {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  quantity: number;
  reorderLevel: number;
}

export type MovementType = 'IN' | 'OUT' | 'ADJUSTMENT' | 'TRANSFER';

export interface StockMovement {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  warehouseId: number;
  warehouseCode: string;
  targetWarehouseId?: number;
  targetWarehouseCode?: string;
  type: MovementType;
  quantity: number;
  reference?: string;
  note?: string;
  createdBy?: string;
  createdAt: string;
}

export interface MovementRequest {
  productId: number;
  warehouseId: number;
  targetWarehouseId?: number | null;
  type: MovementType;
  quantity: number;
  reference?: string;
  note?: string;
}

export interface CategorySlice {
  label: string;
  units: number;
  value: number;
}

export interface WarehouseLoad {
  code: string;
  name: string;
  units: number;
  capacity?: number;
}

export interface DailyFlow {
  date: string;
  inbound: number;
  outbound: number;
}

export interface ProductValue {
  sku: string;
  name: string;
  value: number;
}

export interface ImportResult {
  created: number;
  updated: number;
  errors: string[];
}

export interface AuditEntry {
  id: number;
  actor: string;
  action: string;
  entityType: string;
  entityId?: number;
  details?: string;
  createdAt: string;
}

export interface AnalyticsResponse {
  stockByCategory: CategorySlice[];
  warehouseLoads: WarehouseLoad[];
  movementsDaily: DailyFlow[];
  topProductsByValue: ProductValue[];
}

export type StockStatus = 'ALL' | 'IN' | 'LOW' | 'OUT';

export interface ProductFilters {
  search: string;
  categoryId: number | null;
  supplierId: number | null;
  status: 'ALL' | 'ACTIVE' | 'INACTIVE';
  stockStatus: StockStatus;
  sort: string;
  direction: 'asc' | 'desc';
}

export interface DashboardStats {
  totalProducts: number;
  totalWarehouses: number;
  totalSuppliers: number;
  totalUnits: number;
  stockValue: number;
  lowStock: StockLevel[];
  recentMovements: StockMovement[];
}
