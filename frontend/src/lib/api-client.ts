export class ApiError extends Error {
  readonly status: number;
  readonly error?: string;
  readonly path?: string;

  constructor(message: string, status: number, error?: string, path?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.error = error;
    this.path = path;
  }
}

type ApiRequestOptions = {
  method?: string;
  token?: string;
  body?: unknown;
  headers?: HeadersInit;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readString(value: unknown) {
  return typeof value === "string" ? value : "";
}

async function readJsonSafely(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

function messageFromBody(body: unknown, fallback: string) {
  if (!isRecord(body)) return fallback;
  return readString(body.message) || readString(body.error) || fallback;
}

function errorNameFromBody(body: unknown) {
  return isRecord(body) ? readString(body.error) || undefined : undefined;
}

function pathFromBody(body: unknown) {
  return isRecord(body) ? readString(body.path) || undefined : undefined;
}

export async function apiRequest(path: string, options: ApiRequestOptions = {}): Promise<unknown> {
  const headers = new Headers(options.headers);
  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`);
  }

  const init: RequestInit = {
    method: options.method ?? (options.body === undefined ? "GET" : "POST"),
    headers,
  };

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
    init.body = JSON.stringify(options.body);
  }

  let response: Response;
  try {
    response = await fetch(path, init);
  } catch {
    throw new ApiError("Unable to reach the SmartLab backend.", 0);
  }

  const body = await readJsonSafely(response);
  if (!response.ok) {
    throw new ApiError(
      messageFromBody(body, "The request could not be completed."),
      response.status,
      errorNameFromBody(body),
      pathFromBody(body),
    );
  }

  return body;
}
