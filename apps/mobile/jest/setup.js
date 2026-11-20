// Native safe area insets do not exist under Jest; the library ships a mock with fixed insets.
jest.mock(
  'react-native-safe-area-context',
  () => jest.requireActual('react-native-safe-area-context/jest/mock').default,
);
