package io.github.fupacat;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TSqlCoreRuntimeTest {

    // TRACEABILITY_MAPPING: REQ-010
    @Test
    public void testCoreRuntimeCompilation() {
        TSqlParserToken token = new TSqlParserToken(TSqlTokenType.Select, 0, "SELECT", 1, 1);
        TSqlWhitespaceTokenFilter filter = new TSqlWhitespaceTokenFilter(true, java.util.List.of(token));
        assertThat(filter._getLastToken()).isNotNull();

        ParseError error = new ParseError(100, 0, 1, 1, "test");
        assertThat(error._getNumber()).isEqualTo(100);
        assertThat(error._getMessage()).isEqualTo("test");
    }
}
