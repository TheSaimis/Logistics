import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { InventoryService } from '../../core/inventory.service';
import { AnalyticsResponse } from '../../core/models';

interface Bar {
  label: string;
  value: number;
  height: number; // 0..100 (%)
  display: string;
}

interface HBar {
  label: string;
  sub: string;
  percent: number; // 0..100, capped
  over: boolean;
}

interface FlowChart {
  inPath: string;
  outPath: string;
  maxY: number;
  labels: { x: number; text: string }[];
}

const CHART_W = 600;
const CHART_H = 160;

@Component({
  selector: 'app-admin-analytics',
  imports: [CommonModule],
  templateUrl: './admin-analytics.html',
  styleUrl: './admin-analytics.scss',
})
export class AdminAnalyticsPage implements OnInit {
  private api = inject(InventoryService);

  readonly chartW = CHART_W;
  readonly chartH = CHART_H;

  data = signal<AnalyticsResponse | null>(null);
  error = signal('');

  categoryBars = computed<Bar[]>(() => {
    const slices = this.data()?.stockByCategory ?? [];
    const max = Math.max(...slices.map((s) => s.value), 1);
    return slices.map((s) => ({
      label: s.label,
      value: s.value,
      height: Math.max((s.value / max) * 100, 2),
      display: '€' + Math.round(s.value).toLocaleString(),
    }));
  });

  warehouseBars = computed<HBar[]>(() => {
    const loads = this.data()?.warehouseLoads ?? [];
    return loads.map((w) => {
      const percent = w.capacity ? (w.units / w.capacity) * 100 : 0;
      return {
        label: `${w.code} — ${w.name}`,
        sub: w.capacity
          ? `${w.units.toLocaleString()} / ${w.capacity.toLocaleString()} units (${percent.toFixed(1)}%)`
          : `${w.units.toLocaleString()} units (no capacity set)`,
        percent: Math.min(percent, 100),
        over: percent > 100,
      };
    });
  });

  topProductBars = computed<HBar[]>(() => {
    const products = this.data()?.topProductsByValue ?? [];
    const max = Math.max(...products.map((p) => p.value), 1);
    return products.map((p) => ({
      label: `${p.sku} — ${p.name}`,
      sub: '€' + Math.round(p.value).toLocaleString(),
      percent: (p.value / max) * 100,
      over: false,
    }));
  });

  flow = computed<FlowChart | null>(() => {
    const days = this.data()?.movementsDaily ?? [];
    if (days.length === 0) return null;
    const maxY = Math.max(...days.map((d) => Math.max(d.inbound, d.outbound)), 1);
    const stepX = CHART_W / Math.max(days.length - 1, 1);
    const y = (v: number) => CHART_H - (v / maxY) * (CHART_H - 10);
    const path = (get: (d: { inbound: number; outbound: number }) => number) =>
      days.map((d, i) => `${i === 0 ? 'M' : 'L'}${(i * stepX).toFixed(1)},${y(get(d)).toFixed(1)}`).join(' ');

    const labels = [0, Math.floor(days.length / 2), days.length - 1].map((i) => ({
      x: i * stepX,
      text: days[i].date.slice(5), // MM-DD
    }));

    return { inPath: path((d) => d.inbound), outPath: path((d) => d.outbound), maxY, labels };
  });

  ngOnInit(): void {
    this.api.analytics().subscribe({
      next: (data) => this.data.set(data),
      error: () => this.error.set('Failed to load analytics.'),
    });
  }
}
