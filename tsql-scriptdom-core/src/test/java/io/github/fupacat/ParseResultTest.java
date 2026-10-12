package io.github.fupacat;

import io.github.fupacat.ast.TSqlScript;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ParseResultTest {

    // TRACEABILITY_MAPPING: REQ-021
    @Test
    public void testParseResultWithNullErrorsUsesImmutableEmptyList() {
        ParseResult<TSqlScript> result = new ParseResult<>(null, null);

        assertThat(result.fragment()).isNull();
        assertThat(result.errors()).isEmpty();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.toString()).contains("success=false");
        assertThatThrownBy(() -> result.errors().add(new ParseError(1, 0, 1, 1, "x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // TRACEABILITY_MAPPING: REQ-021
    @Test
    public void testParseResultWithErrorsIsNotSuccess() {
        TSqlScript fragment = new TSqlScript();
        List<ParseError> errors = new ArrayList<>();
        errors.add(new ParseError(102, 0, 1, 1, null));
        ParseResult<TSqlScript> result = new ParseResult<>(fragment, errors);

        assertThat(result.fragment()).isSameAs(fragment);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message).isEmpty();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.toString()).contains("errors=1");
    }
}
