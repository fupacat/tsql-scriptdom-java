package io.github.fupacat;

public class TSqlParserToken extends antlr.CommonToken {
    public int offset;
    public TSqlTokenType tokenType;
    public boolean convertStringToIdentifier;

    public TSqlParserToken() {
        super();
        this.tokenType = TSqlTokenType.None;
    }

    public TSqlParserToken(TSqlTokenType type, String text) {
        this(type, 0, text, 1, 1);
    }

    public TSqlParserToken(TSqlTokenType type, int offset, String text, int line, int column) {
        super(type != null ? type.ordinal() : 0, text);
        this.offset = offset;
        this.tokenType = type != null ? type : TSqlTokenType.None;
        setLine(line);
        setColumn(column);
    }

    // TRACEABILITY_MAPPING: REQ-006
    public int _getOffset() {
        return offset;
    }

    // TRACEABILITY_MAPPING: REQ-006
    public TSqlTokenType _getTokenType() {
        return tokenType;
    }
}
