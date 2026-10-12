package io.github.fupacat;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TSqlParserTokenTest {

    // TRACEABILITY_MAPPING: REQ-006
    @Test
    public void testParserTokenAndTokenType() {
        TSqlParserToken token = new TSqlParserToken(TSqlTokenType.Select, 10, "SELECT", 2, 5);
        assertThat(token._getTokenType()).isEqualTo(TSqlTokenType.Select);
        assertThat(token._getOffset()).isEqualTo(10);
        assertThat(token.getText()).isEqualTo("SELECT");
        assertThat(token.getLine()).isEqualTo(2);
        assertThat(token.getColumn()).isEqualTo(5);

        assertThat(TSqlTokenType.SingleLineComment._isComment()).isTrue();
        assertThat(TSqlTokenType.MultilineComment._isComment()).isTrue();
        assertThat(TSqlTokenType.WhiteSpace._isWhitespaceOrComment()).isTrue();
        assertThat(TSqlTokenType.Select._isWhitespaceOrComment()).isFalse();
    }

    // TRACEABILITY_MAPPING: REQ-006
    @Test
    public void testParserTokenDefaultsWhenTypeIsNull() {
        TSqlParserToken token = new TSqlParserToken(null, 7, "?", 3, 4);

        assertThat(token._getTokenType()).isEqualTo(TSqlTokenType.None);
        assertThat(token.getType()).isZero();
        assertThat(token._getOffset()).isEqualTo(7);
        assertThat(token.getText()).isEqualTo("?");
    }
}
