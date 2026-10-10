package io.github.fupacat;

import io.github.fupacat.ast.TSqlBatch;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TSql160ParserTest {

    // TRACEABILITY_MAPPING: REQ-011
    @Test
    public void testTokenTypesDefined() {
        assertThat(TSqlTokenTypes.Select).isGreaterThan(0);
        assertThat(TSqlTokenTypes.Identifier).isGreaterThan(0);
        assertThat(TSqlTokenTypes.WhiteSpace).isGreaterThan(0);
        assertThat(TSqlTokenTypes.Select).isNotEqualTo(TSqlTokenTypes.Identifier);
    }

    // TRACEABILITY_MAPPING: REQ-012
    @Test
    public void testInternalParserInstantiable() {
        TSql160LexerInternal lexer = new TSql160LexerInternal(new StringReader("SELECT 1;"));
        TSqlWhitespaceTokenFilter filter = new TSqlWhitespaceTokenFilter(true, lexer);
        TSql160ParserInternal parser = new TSql160ParserInternal(filter);
        assertThat(parser).isNotNull();
    }

    // TRACEABILITY_MAPPING: REQ-013, REQ-015
    @Test
    public void testParseValidSelectScript() throws Exception {
        TSql160Parser parser = new TSql160Parser();
        List<ParseError> errors = new ArrayList<>();
        TSqlFragment fragment = parser.parse(new StringReader("SELECT 1;"), errors);

        assertThat(errors).isEmpty();
        assertThat(fragment).isNotNull();
        assertThat(fragment).isInstanceOf(TSqlScript.class);

        TSqlScript script = (TSqlScript) fragment;
        assertThat(script.Batches).isNotNull();
        assertThat(script.Batches).hasSize(1);

        TSqlBatch batch = script.Batches.get(0);
        assertThat(batch.Statements).isNotNull();
        assertThat(batch.Statements).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-013, REQ-015
    @Test
    public void testParseComplexQueryAndDdl() throws Exception {
        TSql160Parser parser = new TSql160Parser();

        List<ParseError> errors1 = new ArrayList<>();
        TSqlFragment selectQuery = parser.parse(new StringReader("SELECT a, b FROM t WHERE a > 10;"), errors1);
        assertThat(errors1).isEmpty();
        assertThat(selectQuery).isNotNull();

        List<ParseError> errors2 = new ArrayList<>();
        TSqlFragment createTable = parser.parse(new StringReader("CREATE TABLE Users (Id INT, Name NVARCHAR(50));"), errors2);
        assertThat(errors2).isEmpty();
        assertThat(createTable).isNotNull();
    }

    // TRACEABILITY_MAPPING: REQ-014, REQ-015
    @Test
    public void testParseInvalidSyntaxCapturesErrors() throws Exception {
        TSql160Parser parser = new TSql160Parser();
        List<ParseError> errors = new ArrayList<>();
        TSqlFragment fragment = parser.parse(new StringReader("SELECT FROM WHERE ;"), errors);

        assertThat(errors).isNotEmpty();
        ParseError error = errors.get(0);
        assertThat(error.number).isEqualTo(102);
        assertThat(error.message).isNotNull();
    }

    // TRACEABILITY_MAPPING: REQ-013
    @Test
    public void testParseNullReaderReturnsNull() throws Exception {
        TSql160Parser parser = new TSql160Parser();
        List<ParseError> errors = new ArrayList<>();
        TSqlFragment fragment = parser.parse(null, errors);
        assertThat(fragment).isNull();
        assertThat(errors).isEmpty();
    }
}
