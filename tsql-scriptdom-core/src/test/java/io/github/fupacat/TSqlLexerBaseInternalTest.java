package io.github.fupacat;

import antlr.Token;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import static org.assertj.core.api.Assertions.assertThat;

public class TSqlLexerBaseInternalTest {

    private static class TestLexer extends TSqlLexerBaseInternal {
        public TestLexer() {
            super();
        }

        @Override
        public Token nextToken() throws antlr.TokenStreamException {
            return null;
        }

        public Token emitToken(int type, String textVal) {
            this.text.setLength(0);
            this.text.append(textVal);
            return _makeToken(type);
        }
    }

    // TRACEABILITY_MAPPING: REQ-008
    @Test
    public void testLexerBaseInternalOffsetTracking() {
        TestLexer lexer = new TestLexer();
        String input = "SELECT 1\nFROM tbl";
        lexer._initializeForNewInput(0, 1, 1, new StringReader(input));

        assertThat(lexer._getCurrentOffset()).isEqualTo(0);
        lexer.setColumn(7); // After "SELECT"
        Token tok = lexer.emitToken(TSqlTokenType.Select.ordinal(), "SELECT");
        assertThat(tok).isInstanceOf(TSqlParserToken.class);
        TSqlParserToken pTok = (TSqlParserToken) tok;
        assertThat(pTok.getText()).isEqualTo("SELECT");
        assertThat(pTok.offset).isEqualTo(0);

        lexer.setColumn(9); // End of line 1
        lexer._newline(); // Advance to line 2
        assertThat(lexer.getLine()).isEqualTo(2);
        assertThat(lexer.getColumn()).isEqualTo(1);
        assertThat(lexer._getCurrentOffset()).isEqualTo(8);
    }
}
