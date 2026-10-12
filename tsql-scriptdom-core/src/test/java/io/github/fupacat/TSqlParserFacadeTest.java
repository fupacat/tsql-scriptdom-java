package io.github.fupacat;

import io.github.fupacat.ast.SelectStatement;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class TSqlParserFacadeTest {

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testParseSimpleValidScript() {
        String sql = "SELECT 1 AS num, 'hello' AS msg FROM dbo.TestTable WHERE id > 10;";
        ParseResult<TSqlScript> result = TSqlParser.parse(sql);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.fragment()).isNotNull();

        // Verify visitor traversal
        AtomicInteger selectCount = new AtomicInteger();
        result.fragment().accept(new TSqlConcreteFragmentVisitor() {
            @Override
            public void visit(SelectStatement statement) {
                selectCount.incrementAndGet();
            }
        });
        assertThat(selectCount.get()).isEqualTo(1);
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testParseInvalidScriptProducesErrors() {
        String sql = "SELECT FROM WHERE;";
        ParseResult<TSqlScript> result = TSqlParser.parse(sql);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).isNotEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testParseWithVersionCompatibilityCheck() {
        // WINDOW clause was introduced in SQL Server 2022 (SQL160).
        // It is supported in modern T-SQL (SQL160+) but invalid when targeting SQL Server 2019 (SQL150).
        String sql = "SELECT c1, SUM(c2) OVER w FROM dbo.t WINDOW w AS (PARTITION BY c1);";
        
        // On SQL180 / SQL160, it passes cleanly
        ParseResult<TSqlScript> modernResult = TSqlParser.parse(sql, SqlVersion.SQL180);
        assertThat(modernResult.isSuccess()).isTrue();

        // On SQL150, compatibility errors should be reported
        ParseResult<TSqlScript> legacyResult = TSqlParser.parse(sql, SqlVersion.SQL150);
        assertThat(legacyResult.hasErrors()).isTrue();
        assertThat(legacyResult.errors().get(0).message).contains("WINDOW clause");
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testBuilderAndReaderParseScriptString() {
        TSqlParser parser = TSqlParser.builder()
                .initialQuotedIdentifiers(true)
                .sqlVersion(SqlVersion.SQL160)
                .build();

        assertThat(parser.getTargetVersion()).isEqualTo(SqlVersion.SQL160);
        assertThat(parser.isInitialQuotedIdentifiersOn()).isTrue();

        ParseResult<TSqlScript> result = parser.parseScript(new StringReader("PRINT 'OK';"));
        assertThat(result.isSuccess()).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testReaderOverloadsUseDefaultAndTargetVersion() {
        ParseResult<TSqlScript> defaultVersionResult = TSqlParser.parse(new StringReader("SELECT 1;"));
        ParseResult<TSqlScript> explicitVersionResult = TSqlParser.parse(new StringReader("SELECT 1;"), SqlVersion.SQL150);

        assertThat(defaultVersionResult.isSuccess()).isTrue();
        assertThat(explicitVersionResult.isSuccess()).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testBuilderFluentMethodsAndNullVersionFallback() {
        TSqlParser parser = TSqlParser.builder()
                .initialQuotedIdentifiers(false)
                .sqlVersion(null)
                .build();

        assertThat(parser.isInitialQuotedIdentifiersOn()).isFalse();
        assertThat(parser.getTargetVersion()).isEqualTo(SqlVersion.SQL180);
        assertThat(parser.parseScript("SELECT 1;").isSuccess()).isTrue();
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testParseScriptHandlesReaderExceptions() {
        TSqlParser parser = new TSqlParser() {
            @Override
            TSql180Parser createUnderlyingParser() {
                return new TSql180Parser() {
                    @Override
                    public TSqlFragment parse(java.io.Reader reader, List errors) {
                        throw new RuntimeException("boom");
                    }
                };
            }
        };

        ParseResult<TSqlScript> result = parser.parseScript(new StringReader("SELECT 1;"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.fragment()).isNull();
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors().get(0).message).contains("Parsing exception: boom");
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testMainShowsHelp() throws Exception {
        MainExecution execution = runMain("-h");

        assertThat(execution.exitCode()).isZero();
        assertThat(execution.stdout()).contains("Usage:");
        assertThat(execution.stderr()).isBlank();
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testMainParsesRawSqlString() throws Exception {
        MainExecution execution = runMain("SELECT", "1;");

        assertThat(execution.exitCode()).isZero();
        assertThat(execution.stdout()).contains("SUCCESS: T-SQL parsed cleanly (0 errors).");
        assertThat(execution.stdout()).contains("Batches:");
        assertThat(execution.stderr()).isBlank();
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testMainReturnsFailureForInvalidSql() throws Exception {
        MainExecution execution = runMain("SELECT", "FROM", "WHERE;");

        assertThat(execution.exitCode()).isEqualTo(1);
        assertThat(execution.stderr()).contains("FAILED: Found");
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testMainReadsSqlFromFile(@TempDir Path tempDir) throws Exception {
        Path sqlFile = tempDir.resolve("valid.sql");
        Files.writeString(sqlFile, "SELECT 1;");

        MainExecution execution = runMain("-f", sqlFile.toString());

        assertThat(execution.exitCode()).isZero();
        assertThat(execution.stdout()).contains("SUCCESS: T-SQL parsed cleanly (0 errors).");
    }

    // TRACEABILITY_MAPPING: REQ-028
    @Test
    public void testMainReturnsErrorForMissingFile(@TempDir Path tempDir) throws Exception {
        Path missingSqlFile = tempDir.resolve("missing.sql");

        MainExecution execution = runMain("-f", missingSqlFile.toString());

        assertThat(execution.exitCode()).isEqualTo(1);
        assertThat(execution.stderr()).contains("Error:");
    }

    // TRACEABILITY_MAPPING: REQ-028
    private MainExecution runMain(String... args) throws Exception {
        String javaBinary = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classPath = System.getProperty("java.class.path");
        List<String> command = new ArrayList<>();
        command.add(javaBinary);
        command.add("-cp");
        command.add(classPath);
        command.add("io.github.fupacat.TSqlParser");
        command.addAll(Arrays.asList(args));

        Process process = new ProcessBuilder(command).start();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        return new MainExecution(exitCode, stdout, stderr);
    }

    private record MainExecution(int exitCode, String stdout, String stderr) {
    }
}
