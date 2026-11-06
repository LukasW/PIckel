package ch.grudligstrasse.pikel.cucumber;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Führt alle Feature-Dateien unter {@code src/test/resources/features} gegen Quarkus mit
 * Dev-Services-PostgreSQL aus. Tag-Routing: {@code @E2E} → Browser-Runner (noch keiner),
 * {@code @Unit} → durch Vitest/JUnit belegt, {@code @Pending} → nicht umgesetzt.
 */
@QuarkusTest
@CucumberOptions(
        features = "classpath:features",
        glue = "ch.grudligstrasse.pikel.cucumber",
        tags = "not @Pending and not @E2E and not @Unit",
        plugin = {"pretty", "html:target/cucumber-reports/report.html"})
public class CucumberIT extends CucumberQuarkusTest {}
