import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiAuthInterceptor } from './api-auth-interceptor';
import { KEYCLOAK_CLIENT } from './auth-service';

describe('apiAuthInterceptor', () => {
  const apiUrl = '/api/v1/auth/me';
  const keycloak = {
    authenticated: true,
    token: 'old-token',
    updateToken: vi.fn<(minValidity: number) => Promise<boolean>>(),
    isTokenExpired: vi.fn<() => boolean>(),
    clearToken: vi.fn<() => Promise<void>>(),
    login: vi.fn<() => Promise<void>>(),
  };

  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.resetAllMocks();
    keycloak.authenticated = true;
    keycloak.token = 'old-token';
    keycloak.updateToken.mockResolvedValue(false);
    keycloak.isTokenExpired.mockReturnValue(false);
    keycloak.clearToken.mockResolvedValue(undefined);
    keycloak.login.mockResolvedValue(undefined);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiAuthInterceptor])),
        provideHttpClientTesting(),
        { provide: KEYCLOAK_CLIENT, useValue: keycloak },
      ],
    });

    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('ergänzt das Bearer-Token bei der eigenen API', async () => {
    const result = firstValueFrom(client.get(apiUrl));
    const request = await vi.waitFor(() => http.expectOne(apiUrl));

    expect(keycloak.updateToken).toHaveBeenCalledWith(30);
    expect(request.request.headers.get('Authorization')).toBe('Bearer old-token');

    request.flush({ subject: 'test-id', username: 'test-user' });
    await expect(result).resolves.toEqual({ subject: 'test-id', username: 'test-user' });
  });

  it.each(['https://example.invalid/api/v1/auth/me', '/apiary/example'])(
    'sendet kein Token an %s',
    async (url) => {
      const result = firstValueFrom(client.get(url));
      const request = http.expectOne(url);

      expect(request.request.headers.has('Authorization')).toBe(false);
      expect(keycloak.updateToken).not.toHaveBeenCalled();

      request.flush({});
      await result;
    },
  );

  it('wartet bei parallelen Anfragen auf das erneuerte Token', async () => {
    const permissionsUrl = '/api/v1/me/permissions';
    let finishRefresh!: (refreshed: boolean) => void;

    keycloak.updateToken.mockReturnValue(
      new Promise<boolean>((resolve) => {
        finishRefresh = resolve;
      }),
    );

    const first = firstValueFrom(client.get(apiUrl));
    const second = firstValueFrom(client.get(permissionsUrl));

    http.expectNone(apiUrl);
    http.expectNone(permissionsUrl);

    keycloak.token = 'new-token';
    finishRefresh(true);

    const firstRequest = await vi.waitFor(() => http.expectOne(apiUrl));
    const secondRequest = await vi.waitFor(() => http.expectOne(permissionsUrl));

    for (const request of [firstRequest, secondRequest]) {
      expect(request.request.headers.get('Authorization')).toBe('Bearer new-token');
      request.flush({});
    }

    await Promise.all([first, second]);
  });

  it('startet nach fehlgeschlagenem Refresh den Login und hält die Anfrage zurück', async () => {
    keycloak.updateToken.mockRejectedValue(new Error('Refresh rejected'));

    await expect(firstValueFrom(client.get(apiUrl))).rejects.toMatchObject({
      kind: 'session',
    });

    http.expectNone(apiUrl);
    expect(keycloak.clearToken).toHaveBeenCalledOnce();
    expect(keycloak.login).toHaveBeenCalledExactlyOnceWith(
      expect.objectContaining({ prompt: 'login' }),
    );
  });

  it('meldet bei 401 einen Sitzungsfehler und wiederholt den Schreibaufruf nicht', async () => {
    const result = firstValueFrom(client.post(apiUrl, {}));
    const request = await vi.waitFor(() => http.expectOne(apiUrl));

    request.flush(null, { status: 401, statusText: 'Unauthorized' });

    await expect(result).rejects.toMatchObject({ kind: 'session' });
    expect(keycloak.clearToken).toHaveBeenCalledOnce();
    expect(keycloak.login).not.toHaveBeenCalled();
    http.expectNone(apiUrl);
  });

  it('meldet bei 403 fehlende Berechtigung', async () => {
    const result = firstValueFrom(client.get(apiUrl));
    const request = await vi.waitFor(() => http.expectOne(apiUrl));

    request.flush(null, { status: 403, statusText: 'Forbidden' });

    await expect(result).rejects.toMatchObject({ kind: 'forbidden' });
    expect(keycloak.clearToken).not.toHaveBeenCalled();
    expect(keycloak.login).not.toHaveBeenCalled();
  });

  it('meldet einen Netzwerkfehler', async () => {
    const result = firstValueFrom(client.get(apiUrl));
    const request = await vi.waitFor(() => http.expectOne(apiUrl));

    request.error(new ProgressEvent('error'));

    await expect(result).rejects.toMatchObject({ kind: 'network' });
  });
});
