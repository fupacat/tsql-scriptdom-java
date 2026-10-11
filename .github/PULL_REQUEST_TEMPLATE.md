## Description
<!-- Provide a brief description of the changes introduced in this PR. -->

## Motivation / Context
<!-- Why is this change required? What issue does it fix? Link to issue using #issue_number. -->

## Checklist
- [ ] Code compiles cleanly and passes all regression tests (`mvn clean test`).
- [ ] Upstream Microsoft regression test pass rate is 100% maintained.
- [ ] No new external runtime dependencies added (zero-dependency core guarantee).
- [ ] Added or updated unit tests covering the changes.
- [ ] Visitor methods in `TSqlFragmentVisitor` / `TSqlConcreteFragmentVisitor` properly updated if AST nodes changed.
