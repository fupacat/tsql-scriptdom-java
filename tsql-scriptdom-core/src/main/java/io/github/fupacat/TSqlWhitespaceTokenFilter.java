package io.github.fupacat;

import antlr.Token;
import antlr.TokenStream;

public class TSqlWhitespaceTokenFilter implements TokenStream {

    private final java.util.List<TSqlParserToken> streamToFilter;
    private int currentTokenIndex = 0;
    private boolean quotedIdentifier = true;
    private TSqlParserToken lastToken;

    public TSqlWhitespaceTokenFilter(boolean quotedIdentifier, java.util.List<TSqlParserToken> streamToFilter) {
        this.quotedIdentifier = quotedIdentifier;
        this.streamToFilter = streamToFilter != null ? streamToFilter : new java.util.ArrayList<>();
        this.currentTokenIndex = 0;
        if (!this.streamToFilter.isEmpty()) {
            this.lastToken = this.streamToFilter.get(0);
        } else {
            this.lastToken = new TSqlParserToken(TSqlTokenType.EndOfFile, null);
        }
    }

    // TRACEABILITY_MAPPING: REQ-007
    @Override
    public Token nextToken() throws antlr.TokenStreamException {
        TSqlParserToken token = null;
        int streamLength = streamToFilter.size();

        while (currentTokenIndex < streamLength) {
            token = streamToFilter.get(currentTokenIndex);
            currentTokenIndex++;

            if (!token.tokenType._isWhitespaceOrComment()) {
                break;
            }
            token = null;
        }

        if (token == null) {
            if (streamLength != 0) {
                TSqlParserToken lastInStream = streamToFilter.get(streamLength - 1);
                int lastLen = lastInStream.getText() == null ? 0 : lastInStream.getText().length();
                token = new TSqlParserToken(
                        TSqlTokenType.EndOfFile,
                        lastInStream.offset + lastLen,
                        null,
                        lastInStream.getLine(),
                        lastInStream.getColumn() + lastLen
                );
            } else {
                token = new TSqlParserToken(TSqlTokenType.EndOfFile, null);
            }
        } else if (token.tokenType == TSqlTokenType.AsciiStringOrQuotedIdentifier) {
            token.convertStringToIdentifier = quotedIdentifier;
        }

        lastToken = token;
        return token;
    }

    // TRACEABILITY_MAPPING: REQ-007
    public boolean _isQuotedIdentifier() {
        return quotedIdentifier;
    }

    // TRACEABILITY_MAPPING: REQ-007
    public TSqlParserToken _getLastToken() {
        return lastToken;
    }
}
