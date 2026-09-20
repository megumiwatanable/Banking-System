package com.banking.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModularMonolithArchitectureTest {
  private static final Path SOURCE_ROOT = Path.of("src/main/java/com/banking");
  private static final Set<String> BUSINESS_MODULES =
      Set.of("account", "asset", "casa", "citad", "credit", "customer", "transaction", "user");
  private static final Set<String> LEGACY_TECHNICAL_PACKAGES =
      Set.of(
          "annotation",
          "config",
          "controller",
          "dto",
          "exception",
          "model",
          "repository",
          "security",
          "service");

  @Test
  void businessCapabilitiesAreTopLevelModules() throws IOException {
    Set<String> actualPackages;
    try (Stream<Path> paths = Files.list(SOURCE_ROOT)) {
      actualPackages =
          paths
              .filter(Files::isDirectory)
              .map(path -> path.getFileName().toString())
              .collect(java.util.stream.Collectors.toSet());
    }

    assertThat(actualPackages).containsAll(BUSINESS_MODULES).contains("shared");
    assertThat(actualPackages).doesNotContainAnyElementsOf(LEGACY_TECHNICAL_PACKAGES);
  }

  @Test
  void apiLayerDoesNotAccessInfrastructureDirectly() throws IOException {
    assertThat(javaFilesInLayer("api"))
        .allSatisfy(
            source ->
                assertThat(read(source))
                    .as("API source %s", source)
                    .doesNotContain(".infrastructure."));
  }

  @Test
  void domainLayerDoesNotDependOnApplicationOrAdapters() throws IOException {
    assertThat(javaFilesInLayer("domain"))
        .allSatisfy(
            source ->
                assertThat(read(source))
                    .as("Domain source %s", source)
                    .doesNotContain(".api.", ".application.", ".infrastructure."));
  }

  @Test
  void sharedCodeDoesNotDependOnBusinessModules() throws IOException {
    List<Path> sharedSources = javaFilesUnder(SOURCE_ROOT.resolve("shared"));

    assertThat(sharedSources)
        .allSatisfy(
            source -> {
              String content = read(source);
              BUSINESS_MODULES.forEach(
                  module ->
                      assertThat(content)
                          .as("Shared source %s", source)
                          .doesNotContain("com.banking." + module + "."));
            });
  }

  private static List<Path> javaFilesInLayer(String layer) throws IOException {
    try (Stream<Path> modules = Files.list(SOURCE_ROOT)) {
      return modules
          .filter(Files::isDirectory)
          .filter(path -> BUSINESS_MODULES.contains(path.getFileName().toString()))
          .map(path -> path.resolve(layer))
          .filter(Files::isDirectory)
          .flatMap(ModularMonolithArchitectureTest::walkUnchecked)
          .filter(path -> path.toString().endsWith(".java"))
          .toList();
    }
  }

  private static List<Path> javaFilesUnder(Path directory) throws IOException {
    try (Stream<Path> paths = Files.walk(directory)) {
      return paths.filter(path -> path.toString().endsWith(".java")).toList();
    }
  }

  private static Stream<Path> walkUnchecked(Path directory) {
    try {
      return Files.walk(directory);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot inspect source directory " + directory, exception);
    }
  }

  private static String read(Path source) {
    try {
      return Files.readString(source);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read source file " + source, exception);
    }
  }
}
