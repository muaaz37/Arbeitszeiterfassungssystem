export const apiBaseUrl = '/api';

export function isOwnApiUrl(url: string, baseUrl: string): boolean {
  try {
    const request = new URL(url, baseUrl);
    const api = new URL(apiBaseUrl, baseUrl);

    return (
      request.origin === api.origin &&
      (request.pathname === api.pathname || request.pathname.startsWith(`${api.pathname}/`))
    );
  } catch {
    return false;
  }
}
