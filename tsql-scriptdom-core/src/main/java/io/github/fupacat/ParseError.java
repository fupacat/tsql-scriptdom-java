package io.github.fupacat;

public class ParseError implements java.io.Serializable {
    public final int number;
    public final int offset;
    public final int line;
    public final int column;
    public final String message;

    public ParseError(int number, int offset, int line, int column, String message) {
        this.number = number;
        this.offset = offset;
        this.line = line;
        this.column = column;
        this.message = message != null ? message : "";
    }

    // TRACEABILITY_MAPPING: REQ-009
    public int _getNumber() {
        return number;
    }

    // TRACEABILITY_MAPPING: REQ-009
    public String _getMessage() {
        return message;
    }
}
