import { Pressable, StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { SafeAreaView } from 'react-native-safe-area-context';
import { supportedLanguages } from '@fcc/i18n';

import { AccountSection } from '@/components/account-section';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { MaxContentWidth, Spacing } from '@/constants/theme';

export default function SettingsScreen() {
  const { t, i18n } = useTranslation();

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.content}>
        <ThemedText type="subtitle">{t('settings.language')}</ThemedText>
        {supportedLanguages.map((language) => {
          const selected = i18n.resolvedLanguage === language;
          return (
            <Pressable
              key={language}
              accessibilityRole="radio"
              accessibilityState={{ selected }}
              onPress={() => void i18n.changeLanguage(language)}
            >
              <ThemedView type={selected ? 'backgroundSelected' : 'backgroundElement'} style={styles.option}>
                <ThemedText>{t(`language.${language}`)}</ThemedText>
              </ThemedView>
            </Pressable>
          );
        })}
        <AccountSection />
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center' },
  content: { flex: 1, width: '100%', maxWidth: MaxContentWidth, padding: Spacing.four, gap: Spacing.two },
  option: { padding: Spacing.three, borderRadius: Spacing.two },
});
