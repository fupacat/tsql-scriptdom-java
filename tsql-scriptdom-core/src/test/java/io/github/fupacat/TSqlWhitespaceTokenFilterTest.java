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

    // TRACEABILITY_MAPPING: REQ-007
    @Test
    public void testWhitespaceTokenFilterSkipsAdjacentCommentsAndComputesEofBoundary() throws Exception {
        List<TSqlParserToken> rawTokens = List.of(
                new TSqlParserToken(TSqlTokenType.WhiteSpace, 0, " ", 1, 1),
                new TSqlParserToken(TSqlTokenType.MultilineComment, 1, "/*a*/", 1, 2),
                new TSqlParserToken(TSqlTokenType.MultilineComment, 6, "/*b*/", 1, 7),
                new TSqlParserToken(TSqlTokenType.Select, 11, "SELECT", 1, 12),
                new TSqlParserToken(TSqlTokenType.WhiteSpace, 17, "\n", 1, 18),
                new TSqlParserToken(TSqlTokenType.SingleLineComment, 18, "--tail", 2, 1)
        );

        TSqlWhitespaceTokenFilter filter = new TSqlWhitespaceTokenFilter(true, rawTokens);

        Token select = filter.nextToken();
        assertThat(select.getText()).isEqualTo("SELECT");

        TSqlParserToken eof = (TSqlParserToken) filter.nextToken();
        assertThat(eof._getTokenType()).isEqualTo(TSqlTokenType.EndOfFile);
        assertThat(eof._getOffset()).isEqualTo(24);
        assertThat(filter._getLastToken()).isSameAs(eof);
    }

    // TRACEABILITY_MAPPING: REQ-007
    @Test
    public void testInnerStreamFilterSkipsWhitespaceAndCommentsUntilEof() throws Exception {
        antlr.TokenStream inner = new antlr.TokenStream() {
            private final List<Token> tokens = List.of(
                    new antlr.CommonToken(TSqlTokenTypes.WhiteSpace, " "),
                    new antlr.CommonToken(TSqlTokenTypes.MultilineComment, "/*c*/"),
                    new antlr.CommonToken(TSqlTokenTypes.SingleLineComment, "--d"),
                    new antlr.CommonToken(TSqlTokenTypes.Select, "SELECT")
            );
            private int index = 0;

            @Override
            public Token nextToken() {
                if (index >= tokens.size()) {
                    return null;
                }
                return tokens.get(index++);
            }
        };

        TSqlWhitespaceTokenFilter filter = new TSqlWhitespaceTokenFilter(true, inner);
        Token token = filter.nextToken();
        assertThat(token.getType()).isEqualTo(TSqlTokenTypes.Select);
        assertThat(token.getText()).isEqualTo("SELECT");
        assertThat(filter._getLastToken()._getTokenType()).isEqualTo(TSqlTokenType.None);

        Token eof = filter.nextToken();
        assertThat(eof.getType()).isEqualTo(TSqlTokenTypes.EOF);
        assertThat(filter._getLastToken()._getTokenType()).isEqualTo(TSqlTokenType.EndOfFile);
    }

    // TRACEABILITY_MAPPING: REQ-007
    @Test
    public void testAsciiStringOrQuotedIdentifierConversionFlagUsesQuotedIdentifierMode() throws Exception {
        TSqlParserToken ambiguous = new TSqlParserToken(TSqlTokenType.AsciiStringOrQuotedIdentifier, 0, "\"abc\"", 1, 1);
        TSqlWhitespaceTokenFilter quotedOn = new TSqlWhitespaceTokenFilter(true, List.of(ambiguous));
        TSqlParserToken onToken = (TSqlParserToken) quotedOn.nextToken();
        assertThat(onToken.convertStringToIdentifier).isTrue();

        TSqlParserToken ambiguousOff = new TSqlParserToken(TSqlTokenType.AsciiStringOrQuotedIdentifier, 0, "\"abc\"", 1, 1);
        TSqlWhitespaceTokenFilter quotedOff = new TSqlWhitespaceTokenFilter(false, List.of(ambiguousOff));
        TSqlParserToken offToken = (TSqlParserToken) quotedOff.nextToken();
        assertThat(offToken.convertStringToIdentifier).isFalse();
    }
}
