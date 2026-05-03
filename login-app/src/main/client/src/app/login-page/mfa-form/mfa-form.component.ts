import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-mfa-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mfa-form.component.html',
})
export class MfaFormComponent {

  @Input() hint: string | null = null;
  @Input() loading = false;
  @Output() submitted = new EventEmitter<string>();

  userResponse = '';

  onSubmit(): void {
    if (this.userResponse.trim()) {
      this.submitted.emit(this.userResponse.trim());
    }
  }
}
