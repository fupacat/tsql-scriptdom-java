package io.github.fupacat;

import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class TSql180ParserTest {

    // TRACEABILITY_MAPPING: REQ-021
    @Test
    public void testParseStandardQuery() throws Exception {
        TSql180Parser parser = new TSql180Parser();
        List<ParseError> errors = new ArrayList<>();
        String sql = "SELECT a, b FROM table1 WHERE a > 10 ORDER BY b DESC;";
        TSqlFragment fragment = parser.parse(new StringReader(sql), errors);

        assertThat(fragment).isNotNull();
        assertThat(fragment).isInstanceOf(TSqlScript.class);
        assertThat(errors).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-021, REQ-023
    @Test
    public void testParseVectorIndexWithVersionCompatibility() throws Exception {
        TSql180Parser parser = new TSql180Parser();
        String sql = "CREATE VECTOR INDEX v_idx ON items(embedding) WITH (METRIC = 'cosine', TYPE = 'DiskANN');";

        // Under SQL180 (no version restriction or target >= 170): passes cleanly
        List<ParseError> errors180 = new ArrayList<>();
        TSqlFragment frag180 = parser._parse(new StringReader(sql), errors180, SqlVersion.SQL180);
        assertThat(frag180).isNotNull();
        assertThat(errors180).isEmpty();

        // Under target SQL160: AST is produced, but compatibility violation is recorded
        List<ParseError> errors160 = new ArrayList<>();
        TSqlFragment frag160 = parser._parse(new StringReader(sql), errors160, SqlVersion.SQL160);
        assertThat(frag160).isNotNull();
        assertThat(errors160).isNotEmpty();
        assertThat(errors160.get(0).message).contains("CREATE VECTOR INDEX");
    }

    // TRACEABILITY_MAPPING: REQ-021, REQ-023
    @Test
    public void testParseReturnsNullForNullReader() throws Exception {
        TSql180Parser parser = new TSql180Parser();
        List<ParseError> errors = new ArrayList<>();

        TSqlFragment fragment = parser.parse(null, errors);

        assertThat(fragment).isNull();
        assertThat(errors).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-021, REQ-023
    @Test
    public void testParseSyntaxErrorIsCollected() throws Exception {
        TSql180Parser parser = new TSql180Parser();
        List<ParseError> errors = new ArrayList<>();

        TSqlFragment fragment = parser.parse(new StringReader("SELECT FROM WHERE;"), errors);

        assertThat(fragment).isNotNull();
        assertThat(errors).isNotEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-021, REQ-023
    @Test
    public void testParseWithQuotedIdentifierToggleConstructors() throws Exception {
        String sql = "SELECT 1;";

        List<ParseError> onErrors = new ArrayList<>();
        TSqlFragment onFragment = new TSql180Parser(true).parse(new StringReader(sql), onErrors);
        assertThat(onFragment).isNotNull();
        assertThat(onErrors).isEmpty();

        List<ParseError> offErrors = new ArrayList<>();
        TSqlFragment offFragment = new TSql180Parser(false).parse(new StringReader(sql), offErrors);
        assertThat(offFragment).isNotNull();
        assertThat(offErrors).isEmpty();
    }
}
