import { type components, unwrap } from '@fcc/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from '@tanstack/react-router';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { api } from '../api';
import { Alert, Button } from '../components/ui';
import { describeError } from '../problem';

type PlatformConnection = components['schemas']['PlatformConnection'];
export type ConnectablePlatform = PlatformConnection['platform'];

const connectionsKey = ['platform-connections'];

const statusTones: Record<PlatformConnection['status'], string> = {
  active: 'bg-green-50 text-green-800 dark:bg-green-950 dark:text-green-200',
  reconnect_required: 'bg-amber-50 text-amber-800 dark:bg-amber-950 dark:text-amber-200',
  not_connected: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
};

/** The platforms whose accounts can be connected; `connected` is the platform that was just connected. */
export function ConnectedAccountsPage({ connected }: { connected?: ConnectablePlatform }) {
  const { t } = useTranslation();
  const connections = useQuery({
    queryKey: connectionsKey,
    queryFn: ({ signal }) => unwrap(api.fetchClient.GET('/api/v1/platform-connections', { signal })),
  });
  const items = connections.data?.items ?? [];

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t('accounts.title')}</h1>
      <p className="text-sm text-gray-600 dark:text-gray-400">{t('accounts.intro')}</p>
      {connected && (
        <Alert tone="success">{t('accounts.connected', { platform: t(`platforms.${connected}`) })}</Alert>
      )}
      {connections.isError && <Alert tone="error">{describeError(connections.error, t).message}</Alert>}
      {connections.isSuccess && items.length === 0 && <Alert tone="info">{t('accounts.none')}</Alert>}
      {items.length > 0 && (
        <ul className="flex flex-col divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white dark:divide-gray-800 dark:border-gray-800 dark:bg-gray-900">
          {items.map((connection) => (
            <ConnectionRow key={connection.platform} connection={connection} />
          ))}
        </ul>
      )}
    </section>
  );
}

function ConnectionRow({ connection }: { connection: PlatformConnection }) {
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();
  const [confirming, setConfirming] = useState(false);
  const platform = t(`platforms.${connection.platform}`);
  const connect = useMutation({
    mutationFn: () =>
      unwrap(
        api.fetchClient.POST('/api/v1/platform-connections/{platform}/authorizations', {
          params: { path: { platform: connection.platform } },
        }),
      ),
    // The platform's consent page sends the user back to the callback route.
    onSuccess: ({ authorizationUrl }) => window.location.assign(authorizationUrl),
  });
  const disconnect = useMutation({
    mutationFn: () =>
      unwrap(
        api.fetchClient.DELETE('/api/v1/platform-connections/{platform}', {
          params: { path: { platform: connection.platform } },
        }),
      ),
    onSuccess: () => {
      setConfirming(false);
      void queryClient.invalidateQueries({ queryKey: connectionsKey });
    },
  });
  const error = connect.error ?? disconnect.error;
  const connectedAt = connection.connectedAt
    ? new Date(connection.connectedAt).toLocaleDateString(i18n.resolvedLanguage)
    : undefined;

  return (
    <li className="flex flex-col gap-3 px-4 py-4">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <span className="font-medium">{platform}</span>
        <span className={`rounded px-2 py-0.5 text-xs font-medium ${statusTones[connection.status]}`}>
          {t(`accounts.status.${connection.status}`)}
        </span>
      </div>
      {connection.accountName && connectedAt && (
        <p className="text-sm text-gray-600 dark:text-gray-400">
          {t('accounts.account', { name: connection.accountName, date: connectedAt })}
        </p>
      )}
      {connection.status === 'reconnect_required' && (
        <p className="text-sm text-amber-800 dark:text-amber-200">
          {t('accounts.reconnectHint', { platform })}
        </p>
      )}
      {error && <Alert tone="error">{describeError(error, t).message}</Alert>}
      <div className="flex flex-wrap gap-2">
        {connection.status !== 'active' && (
          <Button loading={connect.isPending} onClick={() => connect.mutate()}>
            {connection.status === 'not_connected'
              ? t('accounts.connect', { platform })
              : t('accounts.reconnect')}
          </Button>
        )}
        {connection.status !== 'not_connected' &&
          (confirming ? (
            <>
              <Button variant="secondary" loading={disconnect.isPending} onClick={() => disconnect.mutate()}>
                {t('accounts.confirmDisconnect')}
              </Button>
              <Button variant="secondary" onClick={() => setConfirming(false)}>
                {t('accounts.cancel')}
              </Button>
            </>
          ) : (
            <Button variant="secondary" onClick={() => setConfirming(true)}>
              {t('accounts.disconnect')}
            </Button>
          ))}
      </div>
      {confirming && (
        <p className="text-xs text-gray-500 dark:text-gray-400">
          {t('accounts.disconnectHint', { platform })}
        </p>
      )}
    </li>
  );
}

/**
 * Where a platform's consent page returns the user (`/connections/<platform>/callback`): completes the connection
 * with the returned code and state, then shows the accounts page.
 */
export function ConnectionCallbackPage({
  platform,
  code,
  state,
}: {
  platform: ConnectablePlatform;
  code?: string;
  state?: string;
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const complete = useMutation({
    mutationFn: (grant: { code: string; state: string }) =>
      unwrap(
        api.fetchClient.POST('/api/v1/platform-connections/{platform}', {
          params: { path: { platform } },
          body: grant,
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: connectionsKey });
      void navigate({ to: '/accounts', search: { connected: platform }, replace: true });
    },
  });
  const { mutate } = complete;
  // A code can be redeemed once; the ref keeps a second effect run (React strict mode) from sending it again.
  const sent = useRef(false);

  useEffect(() => {
    if (code && state && !sent.current) {
      sent.current = true;
      mutate({ code, state });
    }
  }, [code, state, mutate]);

  const cancelled = !code || !state;

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t('accounts.title')}</h1>
      {cancelled && (
        <Alert tone="info">{t('accounts.cancelled', { platform: t(`platforms.${platform}`) })}</Alert>
      )}
      {!cancelled && !complete.isError && <p className="text-sm">{t('accounts.completing')}</p>}
      {complete.isError && <Alert tone="error">{describeError(complete.error, t).message}</Alert>}
      {(cancelled || complete.isError) && (
        <Link to="/accounts" className="text-sm text-blue-700 hover:underline dark:text-blue-300">
          {t('accounts.back')}
        </Link>
      )}
    </section>
  );
}
