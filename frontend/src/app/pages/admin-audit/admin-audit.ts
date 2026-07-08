import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { InventoryService } from '../../core/inventory.service';
import { AuditEntry, Page } from '../../core/models';

@Component({
  selector: 'app-admin-audit',
  imports: [CommonModule],
  templateUrl: './admin-audit.html',
  styleUrl: './admin-audit.scss',
})
export class AdminAuditPage implements OnInit {
  private api = inject(InventoryService);

  page = signal<Page<AuditEntry> | null>(null);
  error = signal('');
  pageIndex = 0;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.auditLog(this.pageIndex).subscribe({
      next: (page) => this.page.set(page),
      error: () => this.error.set('Failed to load the audit log.'),
    });
  }

  goToPage(index: number): void {
    this.pageIndex = index;
    this.load();
  }

  badgeClass(action: string): string {
    if (action.includes('DELETED') || action.includes('DEACTIVATED')) return 'badge-red';
    if (action.includes('CREATED')) return 'badge-green';
    return 'badge-blue';
  }
}
