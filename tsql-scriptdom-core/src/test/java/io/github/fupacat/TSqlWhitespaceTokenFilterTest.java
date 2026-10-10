package io.github.fupacat;

import antlr.Token;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class TSqlWhitespaceTokenFilterTest {

    // TRACEABILITY_MAPPING: REQ-007
    @Test
    public void testWhitespaceTokenFilter() throws Exception {
        List<TSqlParserToken> rawTokens = List.of(
                new TSqlParserToken(TSqlTokenType.WhiteSpace, 0, "   ", 1, 1),
                new TSqlParserToken(TSqlTokenType.SingleLineComment, 3, "-- note\n", 1, 4),
                new TSqlParserToken(TSqlTokenType.Select, 11, "SELECT", 2, 1),
                new TSqlParserToken(TSqlTokenType.WhiteSpace, 17, " ", 2, 7),
                new TSqlParserToken(TSqlTokenType.IntegerLiteral, 18, "1", 2, 8),
                new TSqlParserToken(TSqlTokenType.Semicolon, 19, ";", 2, 9)
        );

        TSqlWhitespaceTokenFilter filter = new TSqlWhitespaceTokenFilter(true, rawTokens);

        Token t1 = filter.nextToken();
        assertThat(t1.getText()).isEqualTo("SELECT");

        Token t2 = filter.nextToken();
        assertThat(t2.getText()).isEqualTo("1");

        Token t3 = filter.nextToken();
        assertThat(t3.getText()).isEqualTo(";");

        Token eof = filter.nextToken();
        assertThat(eof.getType()).isEqualTo(TSqlTokenType.EndOfFile.ordinal());
    }
}
