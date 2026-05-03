import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login-form.component.html',
})
export class LoginFormComponent {

  @Input() loading = false;
  @Output() submitted = new EventEmitter<{ username: string; password: string }>();

  username = '';
  password = '';

  onSubmit(): void {
    if (this.username.trim() && this.password) {
      this.submitted.emit({ username: this.username.trim(), password: this.password });
    }
  }
}
