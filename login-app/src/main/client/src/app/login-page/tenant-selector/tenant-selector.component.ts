import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-tenant-selector',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tenant-selector.component.html',
})
export class TenantSelectorComponent implements OnInit {

  @Input() tenantIds: string[] = [];
  @Input() loading = false;
  @Output() selected = new EventEmitter<string>();

  selectedTenantId = '';

  ngOnInit(): void {
    if (this.tenantIds.length === 1) {
      this.selectedTenantId = this.tenantIds[0];
    }
  }

  onSubmit(): void {
    if (this.selectedTenantId) {
      this.selected.emit(this.selectedTenantId);
    }
  }
}
