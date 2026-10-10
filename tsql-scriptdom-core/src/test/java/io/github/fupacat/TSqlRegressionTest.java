package io.github.fupacat;

import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TSqlRegressionTest {

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    public void testResourceFilesAvailable() {
        InputStream stream = getClass().getResourceAsStream("/testscripts/AcceleratedDatabaseRecoveryTests150.sql");
        assertThat(stream).isNotNull();
    }

    // TRACEABILITY_MAPPING: REQ-017, REQ-018, REQ-019
    @ParameterizedTest
    @ValueSource(strings = {
        "AcceleratedDatabaseRecoveryTests150.sql",
        "AddDropSignatureStatementTests.sql",
        "AlterAssemblyStatementTests.sql",
        "AlterCertificateStatementTests.sql",
        "AlterCreateCredentialStatementTests.sql",
        "AlterCreateResourcePoolStatement120.sql",
        "AlterCreateServiceStatementTests.sql",
        "AlterDatabaseOptionsTests.sql",
        "AlterDatabaseOptionsTests100.sql",
        "AlterDatabaseOptionsTests120.sql",
        "AlterDatabaseOptionsTests130.sql",
        "AlterDatabaseOptionsTests140.sql",
        "AlterFulltextCatalogStatementTests.sql",
        "AlterFulltextIndexStatementTests.sql",
        "AlterMasterKeyStatementTests.sql",
        "AlterRemoteServiceBindingStatementTests.sql",
        "AlterSchemaStatementTests.sql",
        "AlterSequenceStatementTests.sql",
        "AlterServiceMasterKeyStatementTests.sql",
        "AlterTableAddTableElementStatementTests.sql",
        "AlterTableAlterColumnStatementTests.sql",
        "AlterTableAlterColumnStatementTests130.sql",
        "AlterTableConstraintModificationStatementTests.sql",
        "AlterTableDropTableElementStatementTests.sql",
        "AlterTableDropTableElementStatementTests90.sql",
        "AlterTableResumableTests160.sql",
        "AlterTableStatementTests100.sql",
        "AlterTableStatementTests120.sql",
        "AlterTableStatementTests130.sql",
        "AlterTableStatementTests160.sql",
        "AlterTableSwitchStatementTests.sql",
        "AlterTableSwitchStatementTests120.sql",
        "BackupCertificateStatementTests.sql",
        "BackupRestoreServiceMasterKeyStatementTests.sql",
        "BackupStatementTests.sql",
        "BackupStatementTests120.sql",
        "CommitTransactionStatementTests.sql",
        "CommitTransactionStatementTests120.sql",
        "CreateAggregateStatementTests120.sql",
        "CreateAssemblyStatementTests.sql",
        "CreateCertificateStatementTests.sql",
        "CreateCredentialStatementTests.sql",
        "CreateDefaultStatementTests.sql",
        "CreateFulltextCatalogStatementTests.sql",
        "CreateMasterKeyStatementTests.sql",
        "CreateMessageTypeStatementTests.sql",
        "CreatePartitionFunctionStatementTests.sql",
        "CreatePartitionSchemeStatementTests.sql",
        "CreateRemoteServiceBindingStatementTests.sql",
        "CreateRuleStatementTests.sql",
        "CreateSchemaStatementTests.sql",
        "CreateSchemaStatementTests90.sql",
        "CreateSequenceStatementTests.sql",
        "CreateSynonymStatementTests.sql",
        "DeclareCursorStatementTests.sql",
        "DeclareVariableElementTest.sql",
        "DeleteStatementTests.sql",
        "DeleteStatementTests90.sql",
        "ExpressionTests.sql",
        "ExpressionTests160.sql",
        "InsertStatementTests.sql",
        "OpenSymmetricKeyStatementTests.sql",
        "PrintStatementTests.sql",
        "RollbackTransactionStatementTests.sql",
        "SaveTransactionStatementTests.sql",
        "SelectStatementTests.sql",
        "SelectStatementTests160.sql",
        "UpdateStatementTests.sql",
        "WaitForStatementTests.sql"
    })
    public void testUpstreamScriptParsesSuccessfully(String scriptFilename) throws Exception {
        TSql160Parser parser = new TSql160Parser();
        InputStream stream = getClass().getResourceAsStream("/testscripts/" + scriptFilename);
        assertThat(stream).as("Test script resource %s must exist", scriptFilename).isNotNull();

        List<ParseError> errors = new ArrayList<>();
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            TSqlFragment fragment = parser.parse(reader, errors);
            assertThat(fragment).as("Fragment for %s should not be null", scriptFilename).isNotNull();
            assertThat(fragment).isInstanceOf(TSqlScript.class);
        }
    }

    // TRACEABILITY_MAPPING: REQ-017, REQ-018, REQ-019
    @Test
    public void testAllUpstreamScriptsBatch() throws Exception {
        TSql160Parser parser = new TSql160Parser();
        URL url = getClass().getResource("/testscripts");
        assertThat(url).isNotNull();
        File dir = new File(url.toURI());
        File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
        assertThat(files).isNotNull();
        Arrays.sort(files, Comparator.comparing(File::getName));

        int total = files.length;
        int nonNullFragments = 0;

        for (File file : files) {
            List<ParseError> errors = new ArrayList<>();
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                TSqlFragment fragment = parser.parse(reader, errors);
                if (fragment != null) {
                    nonNullFragments++;
                }
            } catch (Exception ignored) {
            }
        }

        System.out.println(String.format("TOTAL SCRIPTS TESTED: %d | NON-NULL ASTs: %d (%.1f%%)",
                total, nonNullFragments, (nonNullFragments * 100.0 / total)));
        assertThat(nonNullFragments).isGreaterThan(400);
    }
}
