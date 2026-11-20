/** RFC 9457 Problem Details as returned by the API; `code` is stable and translated by the clients. */
export interface ApiProblem {
  status: number;
  code: string;
  detail?: string;
  /** Present when `code` is `validation.failed`. */
  errors?: { field: string; code: string }[];
}

/**
 * Thrown by the session helpers; carries the API problem, or `network.unreachable` when no answer arrived (the
 * underlying error is kept as `cause`).
 */
export class ApiError extends Error {
  readonly problem: ApiProblem;

  constructor(problem: ApiProblem, options?: { cause?: unknown }) {
    super(problem.detail ?? problem.code, options);
    this.name = 'ApiError';
    this.problem = problem;
  }
}

/** Reads the problem from an error response; falls back to a generic code when the body is not a problem. */
export function toProblem(error: unknown, response: Response | undefined): ApiProblem {
  const status = response?.status ?? 0;
  if (error !== null && typeof error === 'object' && typeof (error as ApiProblem).code === 'string') {
    return { ...(error as ApiProblem), status };
  }
  return { status, code: status === 0 ? 'network.unreachable' : 'server.error' };
}

/** Resolves to the response data of a successful call; throws {@link ApiError} for error statuses and outages. */
export async function unwrap<D>(
  request: Promise<{ data?: D; error?: unknown; response: Response }>,
): Promise<D> {
  let result: { data?: D; error?: unknown; response: Response };
  try {
    result = await request;
  } catch (cause) {
    throw new ApiError({ status: 0, code: 'network.unreachable' }, { cause });
  }
  if (!result.response.ok) throw new ApiError(toProblem(result.error, result.response));
  return result.data as D;
}
