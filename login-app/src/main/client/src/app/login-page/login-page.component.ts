import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoginApiService } from '../service/login-api.service';
import { LoginFormComponent } from './login-form/login-form.component';
import { TenantSelectorComponent } from './tenant-selector/tenant-selector.component';
import { MfaFormComponent } from './mfa-form/mfa-form.component';
import {
  LoginRequest,
  LoginResponse,
  LoginStep,
  MfaVerifyRequest,
  TenantSelectRequest,
} from '../model/login.model';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, LoginFormComponent, TenantSelectorComponent, MfaFormComponent],
  templateUrl: './login-page.component.html',
})
export class LoginPageComponent implements OnInit {

  step: LoginStep = 'credentials';
  errorMessage: string | null = null;
  clientName: string | null = null;
  loading = false;

  //-- state carried between steps
  private loginChallenge = '';
  private userId = '';
  private tenantId = '';
  availableTenantIds: string[] = [];
  mfaHint: string | null = null;
  private mfaChallengeToken = '';
  private mfaRequired = false;

  constructor(private readonly loginApi: LoginApiService) {}

  ngOnInit(): void {
    this.loginChallenge = new URLSearchParams(window.location.search).get('login_challenge') ?? '';
    if (!this.loginChallenge) {
      this.errorMessage = 'Missing login_challenge parameter.';
      return;
    }
    this.loginApi.fetchLoginRequest(this.loginChallenge).subscribe({
      next: (req) => {
        this.clientName = req.client?.clientName ?? req.client?.clientId ?? null;
      },
      error: () => {
        this.errorMessage = 'Failed to load login request.';
      },
    });
  }

  onCredentialsSubmit(credentials: { username: string; password: string }): void {
    this.errorMessage = null;
    this.loading = true;
    const request: LoginRequest = {
      loginChallenge: this.loginChallenge,
      username: credentials.username,
      password: credentials.password,
    };
    this.loginApi.submitLogin(request).subscribe({
      next: (response) => this.handleResponse(response),
      error: (err) => this.handleError(err),
    });
  }

  onTenantSelected(selectedTenantId: string): void {
    this.errorMessage = null;
    this.loading = true;
    const request: TenantSelectRequest = {
      loginChallenge: this.loginChallenge,
      userId: this.userId,
      selectedTenantId,
      availableTenantIds: this.availableTenantIds,
      mfaRequired: this.mfaRequired,
    };
    this.loginApi.submitTenantSelection(request).subscribe({
      next: (response) => this.handleResponse(response),
      error: (err) => this.handleError(err),
    });
  }

  onMfaSubmit(userResponse: string): void {
    this.errorMessage = null;
    this.loading = true;
    const request: MfaVerifyRequest = {
      loginChallenge: this.loginChallenge,
      userId: this.userId,
      tenantId: this.tenantId,
      challengeToken: this.mfaChallengeToken,
      userResponse,
    };
    this.loginApi.submitMfaVerification(request).subscribe({
      next: (response) => this.handleResponse(response),
      error: (err) => this.handleError(err),
    });
  }

  private handleResponse(response: LoginResponse): void {
    this.loading = false;
    if (response.redirectTo) {
      window.location.href = response.redirectTo;
      return;
    }
    if (response.tenantSelectionRequired) {
      this.availableTenantIds = response.availableTenantIds ?? [];
      this.mfaRequired = response.mfaRequired;
      this.step = 'tenant-select';
      return;
    }
    if (response.mfaRequired) {
      this.mfaChallengeToken = response.mfaChallengeToken ?? '';
      this.mfaHint = response.mfaHint ?? null;
      this.step = 'mfa';
    }
  }

  private handleError(err: any): void {
    this.loading = false;
    const serverMessage = err?.error?.message ?? err?.error?.detail ?? null;
    this.errorMessage = serverMessage ?? 'Authentication failed. Please try again.';
  }
}
