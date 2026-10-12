package io.github.fupacat;

import antlr.Token;
import antlr.TokenStream;

public class TSqlWhitespaceTokenFilter implements TokenStream {

    private final java.util.List<TSqlParserToken> streamToFilter;
    private final java.util.List<TSqlParserToken> scriptTokenStream = new java.util.ArrayList<>();
    private final java.util.IdentityHashMap<Token, Integer> tokenIndices = new java.util.IdentityHashMap<>();
    private int currentTokenIndex = 0;
    private boolean quotedIdentifier = true;
    public boolean QuotedIdentifier = true;
    private TSqlParserToken lastToken;

    private final TokenStream innerStream;

    public TSqlWhitespaceTokenFilter(boolean quotedIdentifier, java.util.List<TSqlParserToken> streamToFilter) {
        this.quotedIdentifier = quotedIdentifier;
        this.QuotedIdentifier = quotedIdentifier;
        this.streamToFilter = streamToFilter != null ? streamToFilter : new java.util.ArrayList<>();
        this.innerStream = null;
        this.currentTokenIndex = 0;
        if (!this.streamToFilter.isEmpty()) {
            this.lastToken = this.streamToFilter.get(0);
        } else {
            this.lastToken = new TSqlParserToken(TSqlTokenType.EndOfFile, null);
        }
    }

    public TSqlWhitespaceTokenFilter(boolean quotedIdentifier, TokenStream innerStream) {
        this.quotedIdentifier = quotedIdentifier;
        this.QuotedIdentifier = quotedIdentifier;
        this.streamToFilter = null;
        this.innerStream = innerStream;
        this.lastToken = new TSqlParserToken(TSqlTokenType.EndOfFile, null);
    }

    public TSqlWhitespaceTokenFilter(TokenStream innerStream) {
        this(true, innerStream);
    }

    // TRACEABILITY_MAPPING: REQ-007
    @Override
    public Token nextToken() throws antlr.TokenStreamException {
        if (innerStream != null) {
            Token token;
            while ((token = innerStream.nextToken()) != null) {
                int ttype = token.getType();
                if (ttype == TSqlTokenTypes.WhiteSpace || ttype == TSqlTokenTypes.SingleLineComment || ttype == TSqlTokenTypes.MultilineComment) {
                    continue;
                }
                if (token instanceof TSqlParserToken pt) {
                    lastToken = pt;
                } else {
                    lastToken = new TSqlParserToken(TSqlTokenType.None, token.getText());
                    lastToken.setType(token.getType());
                    lastToken.setLine(token.getLine());
                    lastToken.setColumn(token.getColumn());
                }
                tokenIndices.put(token, scriptTokenStream.size());
                scriptTokenStream.add(lastToken);
                return token;
            }
            Token eofToken = new antlr.CommonToken(TSqlTokenTypes.EOF, "");
            lastToken = new TSqlParserToken(TSqlTokenType.EndOfFile, "");
            tokenIndices.put(eofToken, scriptTokenStream.size());
            scriptTokenStream.add(lastToken);
            return eofToken;
        }
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
        tokenIndices.put(token, scriptTokenStream.size());
        scriptTokenStream.add(token);
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

    // TRACEABILITY_MAPPING: REQ-007
    public java.util.List<TSqlParserToken> _getScriptTokenStream() {
        return scriptTokenStream;
    }

    // TRACEABILITY_MAPPING: REQ-007
    public int _getTokenIndex(Token token) {
        Integer index = tokenIndices.get(token);
        return index != null ? index : -1;
    }
}
