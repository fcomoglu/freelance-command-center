import { useSyncExternalStore } from 'react';
import { useColorScheme as useRNColorScheme } from 'react-native';

const subscribe = () => () => {};

/**
 * To support static rendering, the color scheme is only read on the client: the server snapshot is `false`,
 * so the first render matches the statically rendered HTML ('light').
 */
export function useColorScheme() {
  const isClient = useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  );
  const colorScheme = useRNColorScheme();

  return isClient ? colorScheme : 'light';
}
