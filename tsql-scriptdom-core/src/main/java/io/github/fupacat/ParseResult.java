package io.github.fupacat;

import io.github.fupacat.ast.TSqlFragment;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the output of a T-SQL parsing operation, containing the generated
 * Abstract Syntax Tree (AST) fragment and any syntax or compatibility errors encountered.
 *
 * @param <T> The subtype of {@link TSqlFragment} produced (typically {@link io.github.fupacat.ast.TSqlScript}).
 */
public class ParseResult<T extends TSqlFragment> implements Serializable {

    private final T fragment;
    private final List<ParseError> errors;

    public ParseResult(T fragment, List<ParseError> errors) {
        this.fragment = fragment;
        this.errors = errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();
    }

    /**
     * Returns the parsed AST fragment, or {@code null} if parsing failed catastrophically.
     */
    public T fragment() {
        return fragment;
    }

    /**
     * Returns an unmodifiable list of any parse or compatibility errors.
     */
    public List<ParseError> errors() {
        return errors;
    }

    /**
     * Checks if there are any parse or compatibility errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns true if parsing succeeded without any errors and produced a non-null AST.
     */
    public boolean isSuccess() {
        return fragment != null && errors.isEmpty();
    }

    @Override
    public String toString() {
        return "ParseResult{" +
                "success=" + isSuccess() +
                ", hasFragment=" + (fragment != null) +
                ", errors=" + errors.size() +
                '}';
    }
}
