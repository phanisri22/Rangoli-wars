export class ApiError extends Error {
  constructor(message: string, public status: number) {
    super(message);
  }
}

let getToken: () => string | null = () => null;
let onUnauthorized: () => void = () => {};

export function configureApi(tokenGetter: () => string | null, unauthorized: () => void) {
  getToken = tokenGetter;
  onUnauthorized = unauthorized;
}

function fallbackMessage(status: number) {
  if (status === 404) return 'Not found';
  if (status === 403) return 'You are not allowed to do that';
  if (status >= 500) return `The server is unavailable right now (${status})`;
  return `Request failed (${status})`;
}

export async function api<T>(path: string, options: { method?: string; body?: unknown } = {}): Promise<T> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';

  let response: Response;
  try {
    response = await fetch(path, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running?', 0);
  }

  if (response.status === 401 && token) onUnauthorized();

  const text = await response.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }
  if (!response.ok) {
    const message = (data as { message?: string } | null)?.message;
    throw new ApiError(message ?? fallbackMessage(response.status), response.status);
  }
  return data as T;
}

export function errorMessage(e: unknown) {
  return e instanceof Error ? e.message : 'Something went wrong';
}
