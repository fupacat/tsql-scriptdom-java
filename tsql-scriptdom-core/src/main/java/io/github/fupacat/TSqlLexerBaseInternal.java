package io.github.fupacat;

public abstract class TSqlLexerBaseInternal extends antlr.CharScanner {

    public int currentLineStartOffset = 0;
    public int acceptableGoOffset = 0;

    public TSqlLexerBaseInternal() {
        super();
    }

    public TSqlLexerBaseInternal(antlr.LexerSharedInputState state) {
        super(state);
    }

    // TRACEABILITY_MAPPING: REQ-008
    public void _initializeForNewInput(int startOffset, int startLine, int startColumn, java.io.Reader input) {
        if (input != null) {
            this.inputState = new antlr.LexerSharedInputState(new antlr.CharBuffer(input));
        }
        setTabSize(1);
        resetText();
        setColumn(startColumn);
        setLine(startLine);
        this.currentLineStartOffset = startOffset - (startColumn - 1);
        this.acceptableGoOffset = startOffset;
    }

    // TRACEABILITY_MAPPING: REQ-008
    public int _getCurrentOffset() {
        return currentLineStartOffset + getColumn() - 1;
    }

    // TRACEABILITY_MAPPING: REQ-008
    public void _newline() {
        currentLineStartOffset += getColumn() - 1;
        setLine(getLine() + 1);
        setColumn(1);
    }

    // TRACEABILITY_MAPPING: REQ-008
    protected antlr.Token _makeToken(int tokenType) {
        int length = text != null ? text.length() : 0;
        int offset = _getCurrentOffset() - length;
        TSqlTokenType type = TSqlTokenType.CustomToken;
        if (tokenType >= 0 && tokenType < TSqlTokenType.values().length) {
            type = TSqlTokenType.values()[tokenType];
        }
        return new TSqlParserToken(type, offset, getText(), getLine(), getColumn() - length);
    }
}
