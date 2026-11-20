import type { ReactNode } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, TextInput, type TextInputProps, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const ACCENT = '#2563eb';

type TextFieldProps = TextInputProps & { label: string; hint?: string; error?: string };

export function TextField({ label, hint, error, style, ...input }: TextFieldProps) {
  const theme = useTheme();
  return (
    <View style={styles.field}>
      <ThemedText type="smallBold">{label}</ThemedText>
      <TextInput
        accessibilityLabel={label}
        placeholderTextColor={theme.textSecondary}
        style={[
          styles.input,
          {
            color: theme.text,
            backgroundColor: theme.backgroundElement,
            borderColor: error ? '#dc2626' : theme.backgroundSelected,
          },
          style,
        ]}
        {...input}
      />
      {error ? (
        <ThemedText type="small" style={styles.error}>
          {error}
        </ThemedText>
      ) : hint ? (
        <ThemedText type="small" themeColor="textSecondary">
          {hint}
        </ThemedText>
      ) : null}
    </View>
  );
}

type ButtonProps = {
  title: string;
  onPress: () => void;
  loading?: boolean;
  variant?: 'primary' | 'secondary';
};

export function Button({ title, onPress, loading = false, variant = 'primary' }: ButtonProps) {
  const theme = useTheme();
  const primary = variant === 'primary';
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ busy: loading, disabled: loading }}
      disabled={loading}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        primary ? { backgroundColor: ACCENT } : { backgroundColor: theme.backgroundElement },
        (pressed || loading) && styles.dimmed,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={primary ? '#ffffff' : theme.text} />
      ) : (
        <ThemedText type="smallBold" style={primary && styles.primaryText}>
          {title}
        </ThemedText>
      )}
    </Pressable>
  );
}

const noticeColors = {
  error: { backgroundColor: '#fef2f2', color: '#991b1b' },
  success: { backgroundColor: '#f0fdf4', color: '#166534' },
  info: { backgroundColor: '#eff6ff', color: '#1e40af' },
};

export function Notice({ tone, children }: { tone: keyof typeof noticeColors; children: ReactNode }) {
  const { backgroundColor, color } = noticeColors[tone];
  return (
    <View
      accessibilityRole={tone === 'error' ? 'alert' : 'summary'}
      style={[styles.notice, { backgroundColor }]}
    >
      <ThemedText type="small" style={{ color }}>
        {children}
      </ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  field: { gap: Spacing.one },
  input: {
    borderWidth: 1,
    borderRadius: Spacing.two,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    fontSize: 16,
  },
  error: { color: '#dc2626' },
  button: {
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: Spacing.two,
    paddingHorizontal: Spacing.three,
  },
  primaryText: { color: '#ffffff' },
  dimmed: { opacity: 0.7 },
  notice: { borderRadius: Spacing.two, padding: Spacing.three },
});
