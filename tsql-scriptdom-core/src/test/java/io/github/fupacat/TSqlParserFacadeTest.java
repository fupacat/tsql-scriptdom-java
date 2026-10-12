package io.github.fupacat;

import io.github.fupacat.ast.SelectStatement;
import io.github.fupacat.ast.TSqlBatch;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlStatement;
import io.github.fupacat.ast.TSqlScript;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class TSqlParserFacadeTest {

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

    @Test
    public void testParseInvalidScriptProducesErrors() {
        String sql = "SELECT FROM WHERE;";
        ParseResult<TSqlScript> result = TSqlParser.parse(sql);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).isNotEmpty();
    }

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

    @Test
    public void testBuilderAndReader() {
        TSqlParser parser = TSqlParser.builder()
                .initialQuotedIdentifiers(true)
                .sqlVersion(SqlVersion.SQL160)
                .build();

        assertThat(parser.getTargetVersion()).isEqualTo(SqlVersion.SQL160);
        assertThat(parser.isInitialQuotedIdentifiersOn()).isTrue();

        ParseResult<TSqlScript> result = parser.parseScript(new StringReader("PRINT 'OK';"));
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    public void testParsePopulatesScriptTokenStreamAndTokenBounds() {
        ParseResult<TSqlScript> result = TSqlParser.parse("SELECT 1 AS a; SELECT 2 AS b;");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.fragment()).isNotNull();
        assertThat(result.fragment().ScriptTokenStream).isNotEmpty();
        assertThat(result.fragment().lastTokenIndex).isEqualTo(result.fragment().ScriptTokenStream.size() - 1);

        TSqlBatch batch = result.fragment().Batches.get(0);
        assertThat(batch.lastTokenIndex).isGreaterThan(batch.firstTokenIndex);

        TSqlStatement firstStatement = batch.Statements.get(0);
        assertThat(firstStatement.lastTokenIndex).isGreaterThan(0);
        assertThat(firstStatement.lastTokenIndex).isGreaterThanOrEqualTo(firstStatement.firstTokenIndex);
    }
}
