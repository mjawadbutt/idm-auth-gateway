import { Component } from '@angular/core';
import { LoginPageComponent } from './login-page/login-page.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [LoginPageComponent],
  template: '<app-login-page />',
})
export class AppComponent {}
