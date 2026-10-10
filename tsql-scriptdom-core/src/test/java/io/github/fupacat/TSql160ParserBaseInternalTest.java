package io.github.fupacat;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class TSql160ParserBaseInternalTest {

    private static class TestParser extends TSql160ParserBaseInternal {
        public TestParser() {
            super(2);
        }
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    public void testParserBaseInternalRecoveryAndErrors() {
        TestParser parser = new TestParser();
        assertThat(parser.getParseErrors()).isEmpty();

        parser._addParseError(102, 15, 2, 4, "Incorrect syntax near 'FROM'");
        List errors = parser.getParseErrors();
        assertThat(errors).hasSize(1);
        ParseError err = (ParseError) errors.get(0);
        assertThat(err.number).isEqualTo(102);
        assertThat(err.offset).isEqualTo(15);
        assertThat(err.line).isEqualTo(2);
        assertThat(err.column).isEqualTo(4);
        assertThat(err.message).contains("Incorrect syntax");

        assertThat(parser._isStatementRecoveryToken(0)).isTrue();
        assertThat(parser._isBatchRecoveryToken(2)).isTrue();
    }
}
