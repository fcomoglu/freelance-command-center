import type { ConfigContext, ExpoConfig } from 'expo/config';

/**
 * Adds the per-developer push settings to `app.json`; they stay out of the repository.
 * - `EAS_PROJECT_ID`: the Expo project that issues push tokens.
 * - `GOOGLE_SERVICES_JSON`: path to the Firebase config (`google-services.json`) for push on Android.
 */
export default ({ config }: ConfigContext): ExpoConfig => {
  const googleServicesFile = process.env.GOOGLE_SERVICES_JSON;
  const projectId = process.env.EAS_PROJECT_ID;
  return {
    ...config,
    name: config.name ?? 'Freelance Command Center',
    slug: config.slug ?? 'freelance-command-center',
    android: { ...config.android, ...(googleServicesFile ? { googleServicesFile } : {}) },
    extra: { ...config.extra, ...(projectId ? { eas: { projectId } } : {}) },
  };
};
