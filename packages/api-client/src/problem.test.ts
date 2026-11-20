import { describe, expect, it } from 'vitest';

import { ApiError, toProblem, unwrap } from './problem';

const response = (status: number) => new Response(null, { status });

describe('toProblem', () => {
  it('keeps the problem of an error response with the response status', () => {
    const problem = toProblem(
      { code: 'validation.failed', errors: [{ field: 'email', code: 'invalid' }] },
      response(400),
    );

    expect(problem).toEqual({
      status: 400,
      code: 'validation.failed',
      errors: [{ field: 'email', code: 'invalid' }],
    });
  });

  it('falls back to a generic code when the body is not a problem', () => {
    expect(toProblem('<html>Bad Gateway</html>', response(502))).toEqual({
      status: 502,
      code: 'server.error',
    });
    expect(toProblem(undefined, undefined)).toEqual({ status: 0, code: 'network.unreachable' });
  });
});

describe('unwrap', () => {
  it('resolves to the data of a successful call', async () => {
    await expect(unwrap(Promise.resolve({ data: { id: '1' }, response: response(200) }))).resolves.toEqual({
      id: '1',
    });
  });

  it('throws the API problem of an error status', async () => {
    const failure = await unwrap(
      Promise.resolve({ error: { code: 'notification.not_found' }, response: response(404) }),
    ).catch((error: unknown) => error);

    expect(failure).toBeInstanceOf(ApiError);
    expect((failure as ApiError).problem).toEqual({ status: 404, code: 'notification.not_found' });
  });

  it('throws network.unreachable, keeping the cause, when no answer arrives', async () => {
    const cause = new TypeError('Failed to fetch');

    const failure = await unwrap(Promise.reject(cause)).catch((error: unknown) => error);

    expect((failure as ApiError).problem.code).toBe('network.unreachable');
    expect((failure as ApiError).cause).toBe(cause);
  });
});
