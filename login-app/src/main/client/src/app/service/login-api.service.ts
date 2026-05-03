import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  HydraLoginRequest,
  LoginRequest,
  LoginResponse,
  MfaVerifyRequest,
  TenantSelectRequest,
} from '../model/login.model';

const BASE = '/idm-auth-gateway/login';

@Injectable({ providedIn: 'root' })
export class LoginApiService {

  constructor(private readonly http: HttpClient) {}

  fetchLoginRequest(loginChallenge: string): Observable<HydraLoginRequest> {
    const params = new HttpParams().set('login_challenge', loginChallenge);
    return this.http.get<HydraLoginRequest>(BASE, { params });
  }

  submitLogin(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(BASE, request);
  }

  submitTenantSelection(request: TenantSelectRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${BASE}/tenant`, request);
  }

  submitMfaVerification(request: MfaVerifyRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${BASE}/mfa`, request);
  }
}
