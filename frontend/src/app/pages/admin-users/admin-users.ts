import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { InventoryService } from '../../core/inventory.service';
import { UserDto } from '../../core/models';

interface UserForm {
  email: string;
  password: string;
  fullName: string;
  roles: string[];
  enabled: boolean;
}

@Component({
  selector: 'app-admin-users',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsersPage implements OnInit {
  private api = inject(InventoryService);
  readonly auth = inject(AuthService);

  readonly allRoles = ['ADMIN', 'MANAGER', 'VIEWER'];

  users = signal<UserDto[]>([]);
  error = signal('');

  showForm = signal(false);
  editing = signal<UserDto | null>(null);
  form: UserForm = this.emptyForm();

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.users().subscribe({
      next: (users) => this.users.set(users),
      error: () => this.error.set('Failed to load users.'),
    });
  }

  openCreate(): void {
    this.editing.set(null);
    this.form = this.emptyForm();
    this.error.set('');
    this.showForm.set(true);
  }

  openEdit(user: UserDto): void {
    this.editing.set(user);
    this.form = {
      email: user.email,
      password: '',
      fullName: user.fullName,
      roles: [...user.roles],
      enabled: user.enabled,
    };
    this.error.set('');
    this.showForm.set(true);
  }

  toggleRole(role: string): void {
    this.form.roles = this.form.roles.includes(role)
      ? this.form.roles.filter((r) => r !== role)
      : [...this.form.roles, role];
  }

  save(): void {
    const body = {
      email: this.form.email,
      password: this.form.password || null,
      fullName: this.form.fullName,
      roles: this.form.roles,
      enabled: this.form.enabled,
    };
    const editing = this.editing();
    const request = editing ? this.api.updateUser(editing.id, body) : this.api.createUser(body);
    request.subscribe({
      next: () => {
        this.showForm.set(false);
        this.load();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Save failed.'),
    });
  }

  remove(user: UserDto): void {
    if (!confirm(`Delete user ${user.email}?`)) return;
    this.api.deleteUser(user.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(err?.error?.message ?? 'Delete failed.'),
    });
  }

  private emptyForm(): UserForm {
    return { email: '', password: '', fullName: '', roles: ['VIEWER'], enabled: true };
  }
}
