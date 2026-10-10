package io.github.fupacat;

import antlr.LLkParser;
import java.util.List;

public abstract class TSql160ParserBaseInternal extends LLkParser {

    private final List<ParseError> parseErrors = new java.util.ArrayList<>();
    public boolean initialQuotedIdentifiersOn = true;
    public TSqlWhitespaceTokenFilter tokenSource;

    public static final antlr.collections.impl.BitSet STATEMENT_RECOVERY_TOKENS = new antlr.collections.impl.BitSet(new long[]{0x01L, 0x02L});
    public static final antlr.collections.impl.BitSet BATCH_RECOVERY_TOKENS = new antlr.collections.impl.BitSet(new long[]{0x04L, 0x08L});

    public TSql160ParserBaseInternal(int k) {
        super(k);
    }

    public TSql160ParserBaseInternal(antlr.TokenBuffer tokenBuf, int k) {
        super(tokenBuf, k);
    }

    public TSql160ParserBaseInternal(antlr.TokenStream lexer, int k) {
        super(lexer, k);
        if (lexer instanceof TSqlWhitespaceTokenFilter filter) {
            this.tokenSource = filter;
        }
    }

    public TSql160ParserBaseInternal(antlr.ParserSharedInputState state, int k) {
        super(state, k);
    }

    // TRACEABILITY_MAPPING: REQ-009, REQ-010
    @SuppressWarnings("rawtypes")
    public List getParseErrors() {
        return parseErrors;
    }

    // TRACEABILITY_MAPPING: REQ-009
    public void _addParseError(int number, int offset, int line, int column, String message) {
        parseErrors.add(new ParseError(number, offset, line, column, message));
    }

    // TRACEABILITY_MAPPING: REQ-009
    public boolean _isStatementRecoveryToken(int tokenType) {
        return STATEMENT_RECOVERY_TOKENS.member(tokenType);
    }

    // TRACEABILITY_MAPPING: REQ-009
    public boolean _isBatchRecoveryToken(int tokenType) {
        return BATCH_RECOVERY_TOKENS.member(tokenType);
    }
}
