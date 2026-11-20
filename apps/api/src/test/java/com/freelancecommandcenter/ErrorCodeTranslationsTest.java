package com.freelancecommandcenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every error code the API can return has a translation under {@code errors.} in each client locale, so clients never
 * fall back to the generic message for a known error. The codes are read from the sources: the code argument of every
 * {@code DomainException}, the fixed codes of the problem handler, and {@code validation.<constraint>} for every Bean
 * Validation constraint in use. The {@code request.<status>} codes of framework errors (malformed JSON, unknown path)
 * mean a client bug and are left to the generic message.
 */
class ErrorCodeTranslationsTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Path LOCALES = Path.of("../../packages/i18n/locales");

    private static final Pattern DOMAIN_CODE = Pattern.compile("ErrorKind\\.[A-Z_]+,\\s*\"([a-z_]+\\.[a-z_]+)\"");
    private static final Pattern HANDLER_CODE =
            Pattern.compile("setProperty\\(\\s*\"code\",\\s*\"([a-z_]+\\.[a-z_]+)\"");
    private static final Pattern CONSTRAINT = Pattern.compile("import jakarta\\.validation\\.constraints\\.(\\w+);");

    @ParameterizedTest
    @ValueSource(strings = {"en", "tr"})
    void everyErrorCodeIsTranslated(String language) {
        JsonNode errors =
                readJson(LOCALES.resolve(language).resolve("common.json")).path("errors");

        Set<String> codes = errorCodes();

        assertThat(codes).contains("identity.invalid_credentials", "validation.failed", "validation.not_blank");
        assertThat(codes.stream().filter(code -> !translated(errors, code)).toList())
                .as("error codes without a translation in %s", language)
                .isEmpty();
    }

    private static Set<String> errorCodes() {
        Set<String> codes = new TreeSet<>();
        for (String source : sources()) {
            collect(DOMAIN_CODE, source, codes, code -> code);
            collect(HANDLER_CODE, source, codes, code -> code);
            collect(CONSTRAINT, source, codes, constraint -> "validation." + snakeCase(constraint));
        }
        return codes;
    }

    private static void collect(
            Pattern pattern, String source, Set<String> codes, java.util.function.UnaryOperator<String> toCode) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            codes.add(toCode.apply(matcher.group(1)));
        }
    }

    private static boolean translated(JsonNode errors, String code) {
        JsonNode node = errors;
        for (String part : code.split("\\.")) {
            node = node.path(part);
        }
        return node.isString() && !node.asString().isBlank();
    }

    /** Mirrors the problem handler: {@code NotBlank} becomes {@code not_blank}. */
    private static String snakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static List<String> sources() {
        try (Stream<Path> files = Files.walk(SOURCES)) {
            return files.filter(file -> file.toString().endsWith(".java"))
                    .map(ErrorCodeTranslationsTest::read)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static JsonNode readJson(Path file) {
        return JsonMapper.builder().build().readTree(read(file));
    }
}
