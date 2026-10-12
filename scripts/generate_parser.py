#!/usr/bin/env python3
import sys
import os
import re
import shutil
import subprocess
from pathlib import Path

def main():
    repo_root = Path(__file__).resolve().parent.parent
    core_dir = repo_root / "tsql-scriptdom-core"
    astgen_dir = repo_root / "tsql-scriptdom-astgen"
    
    ast_out = core_dir / "target/generated-sources/ast"
    antlr_out = core_dir / "target/generated-sources/antlr"
    
    os.makedirs(ast_out, exist_ok=True)
    if antlr_out.exists():
        shutil.rmtree(antlr_out)
    os.makedirs(antlr_out, exist_ok=True)
    
    # 1. Compile AstGenRunner if needed and run AST generator
    print("Step 1: Generating AST classes...")
    javapoet_jar = Path.home() / ".m2/repository/com/squareup/javapoet/1.13.0/javapoet-1.13.0.jar"
    antlr_jar = Path.home() / ".m2/repository/antlr/antlr/2.7.7/antlr-2.7.7.jar"
    
    # Find javac and java
    javac_cmd = "javac"
    java_cmd = "java"
    jvm_current = Path.home() / ".local/share/jvm/current/bin"
    if (jvm_current / "javac").exists():
        javac_cmd = str(jvm_current / "javac")
        java_cmd = str(jvm_current / "java")

    runner_bin = repo_root / "target/runner_bin"
    os.makedirs(runner_bin, exist_ok=True)
    
    cp_build = f"{astgen_dir}/target/classes:{javapoet_jar}:{core_dir}/src/main/resources"
    subprocess.check_call([javac_cmd, "-cp", cp_build, "-d", str(runner_bin), str(repo_root / "scripts/AstGenRunner.java")])
    
    cp_run = f"{runner_bin}:{astgen_dir}/target/classes:{javapoet_jar}:{core_dir}/src/main/resources"
    subprocess.check_call([java_cmd, "-cp", cp_run, "scripts.AstGenRunner", str(ast_out)])
    
    # 2. Inspect TSql180.g for AST enum constants and update AST enums
    print("Step 2: Updating AST enums with referenced constants...")
    grammar_src = (core_dir / "src/main/resources/grammar/TSql180.g").read_text(encoding="utf-8")
    
    matches = re.findall(r'\b([A-Z][a-zA-Z0-9_]*)\.([A-Z][a-zA-Z0-9_]*)\b', grammar_src)
    skip = {'System', 'String', 'Integer', 'Boolean', 'Token', 'Math', 'FragmentFactory',
            'Debug', 'TSql160ParserInternal', 'TSql180ParserInternal', 'Arrays', 'Collections', 'List',
            'Int32', 'Decimal', 'SensitivityClassification',
            'PhaseOnePartialAstException', 'PhaseOneConstraintException',
            'TSql90ParserBaseInternal', 'ExternalFileOption'}
    ast_enum_dir = ast_out / "io/github/fupacat/ast"
    ast_enums = {}
    for e, c in matches:
        if e not in skip:
            ast_enums.setdefault(e, set()).add(c)
    ast_enums.setdefault('SensitivityClassificationOptionType', set()).update(
        {'Rank', 'Label', 'InformationType', 'LabelId', 'InformationTypeId'}
    )
    ast_enums.setdefault('SqlDataTypeOption', set()).update({
        'None', 'BigInt', 'Binary', 'Bit', 'Char', 'Cursor', 'Date', 'DateTime',
        'DateTime2', 'DateTimeOffset', 'Decimal', 'Float', 'Geography', 'Geometry',
        'HierarchyId', 'Image', 'Int', 'Money', 'NChar', 'NText', 'Numeric',
        'NVarChar', 'Real', 'Rowversion', 'SmallDateTime', 'SmallInt', 'SmallMoney',
        'Sql_Variant', 'Table', 'Text', 'Time', 'Timestamp', 'TinyInt',
        'UniqueIdentifier', 'VarBinary', 'VarChar', 'Xml'
    })
            
    for e_name, consts in ast_enums.items():
        f = ast_enum_dir / f"{e_name}.java"
        if f.exists():
            text = f.read_text(encoding="utf-8")
            if "public enum " in text:
                consts_str = ", ".join(["DEFAULT"] + sorted(list(consts)))
                new_text = re.sub(r'public enum ' + e_name + r'\s*\{[^}]*\}',
                                  f'public enum {e_name} {{\n    {consts_str};\n}}', text)
                f.write_text(new_text, encoding="utf-8")

    # 3. Discover and generate non-AST helpers, enums, exceptions, and resources
    print("Step 3: Generating support classes in antlr directory...")
    
    # 3a. CodeGenerationSupporter
    cs_supporter_file = core_dir / "src/main/resources/grammar/CodeGenerationSupporter.cs"
    consts_dict = {}
    if cs_supporter_file.exists():
        cs_content = cs_supporter_file.read_text(encoding="utf-8")
        consts_dict = dict(re.findall(r'internal const string\s+([A-Za-z0-9_]+)\s*=\s*(\"[^\"]*\");', cs_content))

    supporter_fields = []
    sup_matches = re.findall(r'CodeGenerationSupporter\.([A-Za-z0-9_]+)', grammar_src)
    for name in sorted(set(sup_matches)):
        val = consts_dict.get(name, f'"{name}"')
        supporter_fields.append(f'    public static final String {name} = {val};')
    (antlr_out / "CodeGenerationSupporter.java").write_text(
        f'package io.github.fupacat;\npublic class CodeGenerationSupporter {{\n' + "\n".join(supporter_fields) + '\n}\n',
        encoding="utf-8"
    )

    
    # 3b. TSqlParserResource
    res_fields = []
    res_matches = re.findall(r'TSqlParserResource\.([A-Za-z0-9_]+)', grammar_src)
    for name in sorted(set(res_matches)):
        res_fields.append(f'    public static final String {name} = "{name}";')
    (antlr_out / "TSqlParserResource.java").write_text(
        f'package io.github.fupacat;\npublic class TSqlParserResource {{\n' + "\n".join(res_fields) + '\n}\n',
        encoding="utf-8"
    )

    # 3c. TSqlParseErrorException
    (antlr_out / "TSqlParseErrorException.java").write_text(
        '''package io.github.fupacat;

public class TSqlParseErrorException extends RuntimeException {
    public boolean DoNotLog = false;
    public ParseError ParseError = new ParseError(102, 0, 1, 1, "Syntax error");
    public Object Statement;
    public antlr.Token token;
    public TSqlParseErrorException() { super(); }
    public TSqlParseErrorException(String msg) { super(msg); }
}
''', encoding="utf-8"
    )

    (antlr_out / "PhaseOnePartialAstException.java").write_text(
        '''package io.github.fupacat;

public class PhaseOnePartialAstException extends RuntimeException {
    public Object Statement;
    public PhaseOnePartialAstException() { super(); }
    public PhaseOnePartialAstException(String msg) { super(msg); }
}
''', encoding="utf-8"
    )

    (antlr_out / "PhaseOneConstraintException.java").write_text(
        '''package io.github.fupacat;

public class PhaseOneConstraintException extends RuntimeException {
    public Object Statement;
    public io.github.fupacat.ast.ConstraintDefinition Constraint;
    public PhaseOneConstraintException() { super(); }
    public PhaseOneConstraintException(String msg) { super(msg); }
}
''', encoding="utf-8"
    )

    (antlr_out / "TSql90ParserBaseInternal.java").write_text(
        '''package io.github.fupacat;

public class TSql90ParserBaseInternal {
    public static final long BulkInsertOptionsProhibitedInOpenRowset = 0L;
}
''', encoding="utf-8"
    )

    (antlr_out / "SqlCompatibilityInternalVisitor.java").write_text(
        '''package io.github.fupacat;

import io.github.fupacat.ast.CreateVectorIndexStatement;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TemporalClause;
import io.github.fupacat.ast.VectorDataTypeReference;
import io.github.fupacat.ast.WindowClause;
import java.util.ArrayList;
import java.util.List;

public class SqlCompatibilityInternalVisitor extends TSqlConcreteFragmentVisitor {

    private final SqlVersion targetVersion;
    private final List<ParseError> errors = new ArrayList<>();

    public SqlCompatibilityInternalVisitor(SqlVersion targetVersion) {
        this.targetVersion = targetVersion != null ? targetVersion : SqlVersion.SQL180;
    }

    @SuppressWarnings("rawtypes")
    public static List validate(TSqlFragment fragment, SqlVersion targetVersion) {
        SqlCompatibilityInternalVisitor visitor = new SqlCompatibilityInternalVisitor(targetVersion);
        if (fragment != null) {
            fragment.accept(visitor);
        }
        return visitor.errors;
    }

    @Override
    public void visit(CreateVectorIndexStatement node) {
        if (targetVersion.getCompatibilityLevel() < 170) {
            errors.add(new ParseError(46001, node != null ? node.firstTokenIndex : 0, 1, 1,
                    "Feature 'CREATE VECTOR INDEX' requires SQL Server 2025 (compatibility level 170) or higher. Target is compatibility level "
                            + targetVersion.getCompatibilityLevel() + "."));
        }
        super.visit(node);
    }

    @Override
    public void visit(VectorDataTypeReference node) {
        if (targetVersion.getCompatibilityLevel() < 170) {
            errors.add(new ParseError(46002, node != null ? node.firstTokenIndex : 0, 1, 1,
                    "Feature 'VECTOR data type' requires SQL Server 2025 (compatibility level 170) or higher. Target is compatibility level "
                            + targetVersion.getCompatibilityLevel() + "."));
        }
        super.visit(node);
    }

    @Override
    public void visit(WindowClause node) {
        if (targetVersion.getCompatibilityLevel() < 160) {
            errors.add(new ParseError(46003, node != null ? node.firstTokenIndex : 0, 1, 1,
                    "Feature 'WINDOW clause' requires SQL Server 2022 (compatibility level 160) or higher. Target is compatibility level "
                            + targetVersion.getCompatibilityLevel() + "."));
        }
        super.visit(node);
    }

    @Override
    public void visit(TemporalClause node) {
        if (targetVersion.getCompatibilityLevel() < 130) {
            errors.add(new ParseError(46004, node != null ? node.firstTokenIndex : 0, 1, 1,
                    "Feature 'FOR SYSTEM_TIME temporal clause' requires SQL Server 2016 (compatibility level 130) or higher. Target is compatibility level "
                            + targetVersion.getCompatibilityLevel() + "."));
        }
        super.visit(node);
    }
}
''', encoding="utf-8"
    )

    # 3d. TSqlLexerBaseInternalHelper
    (antlr_out / "TSqlLexerBaseInternalHelper.java").write_text(
        '''package io.github.fupacat;

public abstract class TSqlLexerBaseInternalHelper extends TSqlLexerBaseInternal {
    public int CurrentOffset = 0;
    public int _acceptableGoOffset = 0;

    public TSqlLexerBaseInternalHelper() { super(); }
    public TSqlLexerBaseInternalHelper(antlr.LexerSharedInputState state) { super(state); }
    public TSqlLexerBaseInternalHelper(antlr.InputBuffer ib) { this(new antlr.LexerSharedInputState(ib)); }

    public boolean IsValueTooLargeForTokenInteger(String s) {
        try { Long.parseLong(s); return false; } catch (Exception e) { return true; }
    }
    public void beginComplexToken() {}
    public void checkEOF(Object kind) throws antlr.RecognitionException, antlr.CharStreamException, antlr.TokenStreamException {
        if (LA(1) == EOF_CHAR || LA(1) == (char)-1 || LA(1) == '\uFFFF') {
            throw new antlr.NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
        }
    }
}
''', encoding="utf-8"
    )

    # 3e. TSql180ParserBaseInternalHelper
    helper_methods = [
        'CreateAutoCleanupDetail', 'CreateEventGroupContainer', 'CreateEventTypeContainer',
        'CreateIdentifierOrScalarExpressionFromIdentifier', 'CreateInternalError', 'CreateSetClauseColumn',
        'CreateSimpleRestoreOptionWithValue', 'CreateStopRestoreOption', 'GetAlterIndexStatementKind',
        'GetOptionTypeByName', 'IdentifierOrValueExpression', 'NormalizeDatePartFunctionFirstParameter',
        'ParseAlterCreateFunctionWithOption', 'ParseCreateStatisticsWithOption', 'ParseIndexLegacyWithOption',
        'ParseJoinOptimizerHint', 'ParseOdbcLiteralType', 'ParseOption', 'ParseSampleOptionsWithOption',
        'ParseSecurityObjectKind', 'ParseTriggerEnforcement', 'ParseUnionOptimizerHint',
        'ProcessNationalAndVarying', 'GetFirstToken',
        'AggregateAuthenticationType', 'AggregatePortType',
        'RecognizeAesOrRc4', 'RecognizeAuthenticationProtocol',
        'ParseSecurityObjectKindTSql170'
    ]
    string_methods = [
        'Unquote', 'DecodeAsciiStringLiteral', 'DecodeUnicodeStringLiteral'
    ]
    boolean_methods = [
        'NextIdentifierMatchesOneOf',
        'IsStopAtBeforeMarkRestoreOption', 'IsGraphPseudoColumn',
        'IsAsciiStringLob', 'IsBinaryLiteralLob', 'IsUnicodeStringLob',
        'Match', 'MatchString', 'OptionValidForCreateDatabase',
        'RecognizeAlterLoginSecAdminPasswordOption',
        'RequiresEqualsSign', 'SplitNumericIntoIpParts', 'TryMatch', 'TryParse',
        'TryParseOption'
    ]
    void_methods = [
        'initialize', 'beginComplexToken', 'checkEOF',
        'UpdatePositionInfo', 'AddParseError',
        'ThrowParseErrorException', 'ThrowIncorrectSyntaxErrorException',
        'ThrowPartialAstIfPhaseOne', 'ThrowConstraintIfPhaseOne',
        'ThrowSyntaxErrorIfNotCreateAlterTable', 'ResetQuotedIdentifiersSettingToInitial',
        'AddConstraintToColumn',
        'AddConstraintToComputedColumn',
        'AssignCopyColumnOptions', 'AssignValueToCopyOption',
        'CheckAndIncrementColumnCount', 'CheckBoundingBoxParameterDuplication',
        'CheckBrokerPriorityParameterDuplication', 'CheckCertificateOptionDupication',
        'CheckComparisonOperandForIndexFilter', 'CheckCopyOptionDuplication',
        'CheckCtasStatementHasDistributionOption', 'CheckDmlTriggerActionDuplication',
        'CheckExternalTableCtasStatementHasNotRejectedRowLocationOption',
        'CheckFillFactorRange', 'CheckForCellsPerObjectValueRange',
        'CheckForConflictingOptionsInOpenRowsetBulkCosmos',
        'CheckForDataFileFormatProhibitedOptionsBulkInsert',
        'CheckForDataFileFormatProhibitedOptionsInOpenRowsetBulk',
        'CheckForDistinctInWindowedAggregate',
        'CheckForParquetFormatProhibitedOptionsInOpenRowsetBulk',
        'CheckForTemporaryFunction', 'CheckForTemporaryView',
        'CheckHekatonTableForInlineFilteredIndexes',
        'CheckHekatonTableForNonClusteredColumnStoreIndexes', 'CheckIdentifierLength',
        'CheckIdentifierLiteralLength', 'CheckIfEndpointOptionAllowed',
        'CheckIfPayloadOptionAllowed', 'CheckIfValidLanguageHex',
        'CheckIfValidLanguageIdentifier', 'CheckIfValidLanguageInteger',
        'CheckIfValidLanguageString', 'CheckIfValidSpatialIndexOptionValue',
        'CheckLowPriorityLockWaitValue', 'CheckPartitionAllSpecifiedForIndexRebuild',
        'CheckRetentionPeriodDuration', 'CheckSpecialColumn', 'CheckSqlDataTypeParameters',
        'CheckStarQualifier', 'CheckTableNameExistsForColumn',
        'CheckTemporalPeriodInTableDefinition', 'CheckTwoPartNameForSchemaObjectName',
        'CheckValidWlmTimeLiteral', 'CheckWindowFrame', 'CheckXMLValidity',
        'CheckXmlForClauseOptionDuplication',
        'GetIPv4FragmentsFromNumberDotNumberNumeric', 'PutIdentifiersIntoFunctionCall',
        'SetFileStreamStorageOption', 'SetFunctionBodyStatement', 'SetIdentifier',
        'SetNameForDoublePrecisionType', 'SetSparseStorageOption',
        'SetUnquotedIdentifier', 'ThrowIfCompressionDelayValueOutOfRange',
        'ThrowIfEndOfFileOrBatch', 'ThrowIfInvalidListenerPortValue',
        'ThrowIfMaxdopValueOutOfRange', 'ThrowIfPercentValueOutOfRange',
        'ThrowIfTooLargeAuditFileSize', 'ThrowIfWrongGuidFormat',
        'UpdateBoundingBoxParameterEncounteredOptions',
        'UpdateBrokerPriorityEncounteredOptions',
        'UpdateDmlTriggerActionEncounteredOptions', 'ValidateCopyCredential',
        'ValidateSecret', 'VerifyAllowedIndexOption', 'VerifyAllowedIndexOption160',
        'VerifyColumnDataType', 'CheckOptionDuplication',
        'ValidateFetchApproximate', 'ValidateSemanticIndexExternalModel'
    ]

    h_lines = [
        'package io.github.fupacat;',
        'import antlr.Token;',
        'import io.github.fupacat.ast.*;',
        'import java.util.*;',
        '@SuppressWarnings({"unchecked", "rawtypes"})',
        'public class TSql180ParserBaseInternalHelper extends TSql160ParserBaseInternal implements TSql180ParserInternalTokenTypes {',
        '    public TSqlWhitespaceTokenFilter _tokenSource;',
        '    public boolean PhaseOne = false;',
        '    public int _acceptableGoOffset = 0;',
        '    public int CurrentOffset = 0;',
        '    public static class Debug { public static void Assert(boolean c) {} public static void Assert(boolean c, String m) {} }',
        '    public static final Object Undefined = null;',
        '    public int _ordinal(Object obj) { return (obj instanceof java.lang.Enum<?> e) ? e.ordinal() : 0; }',
        '    public TSql180ParserBaseInternalHelper(boolean initialQuotedIdentifiersOn) { super(2); this.initialQuotedIdentifiersOn = initialQuotedIdentifiersOn; }',
        '    public TSql180ParserBaseInternalHelper(antlr.TokenBuffer b, int k) { super(b, k); }',
        '    public TSql180ParserBaseInternalHelper(antlr.TokenBuffer b) { super(b, 2); }',
        '    public TSql180ParserBaseInternalHelper(antlr.TokenStream l, int k) { super(l, k); if (l instanceof TSqlWhitespaceTokenFilter filter) { this._tokenSource = filter; } }',
        '    public TSql180ParserBaseInternalHelper(antlr.ParserSharedInputState s, int k) { super(s, k); }',
        '    public ParseError GetUnexpectedTokenError(Object... args) { int line = 1, col = 1; String text = ""; try { Token tok = LT(1); if (tok != null) { line = tok.getLine(); col = tok.getColumn(); text = tok.getText(); } } catch (Exception ignored) {} return new ParseError(102, 0, line, col, "Syntax error near " + text); }',
        '    public RuntimeException GetUnexpectedTokenErrorException(Object... args) { return new TSqlParseErrorException(); }',
        '    public ParseError GetFaultTolerantUnexpectedTokenError(Object... args) { if (args.length > 0 && args[0] instanceof Token tok) { return new ParseError(102, 0, tok.getLine(), tok.getColumn(), "Unexpected token: " + tok.getText()); } return GetUnexpectedTokenError(args); }',
        '    public ParseError ProcessTokenStreamRecognitionException(Object... args) { return GetUnexpectedTokenError(args); }',
        '    public void AddParseError(Object error) { if (System.getProperty("DEBUG_PARSE") != null) new Exception("AddParseError: " + error).printStackTrace(); if (error instanceof ParseError e) getParseErrors().add(e); }',
        '    public Identifier CreateIdentifierFromToken(Token t) { Identifier id = new Identifier(); if (t != null) id.Value = t.getText(); return id; }',
        '    public Identifier CreateIdentifierFromLabel(Token t) { return CreateIdentifierFromToken(t); }',
        '    public void CreateIdentifierFromLabel(Token label, Identifier identifier, MultiPartIdentifier multiPartIdentifier) { if (identifier != null && label != null) identifier.Value = label.getText(); if (multiPartIdentifier != null && identifier != null) multiPartIdentifier.Identifiers.add(identifier); }',
        '    public Identifier GetEmptyIdentifier(Object... args) { return new Identifier(); }',
        '    public Literal GetIPv4FragmentFromDotNumberNumeric(Object... args) { return new IntegerLiteral(); }',
        '    public Literal GetIPv4FragmentFromNumberDotNumeric(Object... args) { return new IntegerLiteral(); }',
        '    public void AddAndUpdateTokenInfo(Object... args) { if (args.length >= 3 && args[1] instanceof java.util.List list && args[2] != null) { if (args[2] instanceof java.util.Collection<?> c) { list.addAll(c); for (Object item : c) { UpdateTokenInfo(args[0], item); } } else { list.add(args[2]); UpdateTokenInfo(args[0], args[2]); } if (args[0] instanceof SchemaObjectName son) { int count = son.Identifiers.size(); if (count > 0) son.BaseIdentifier = son.Identifiers.get(count - 1); if (count > 1) son.SchemaIdentifier = son.Identifiers.get(count - 2); if (count > 2) son.DatabaseIdentifier = son.Identifiers.get(count - 3); if (count > 3) son.ServerIdentifier = son.Identifiers.get(count - 4); } } }',
        '    public int ResolveTokenIndex(Object tokenLike) { if (_tokenSource == null || tokenLike == null) return -1; if (tokenLike instanceof Token tok) { return _tokenSource._getTokenIndex(tok); } return -1; }',
        '    public void AttachScriptTokenStream(Object fragment) { if (_tokenSource == null || !(fragment instanceof TSqlFragment f) || f.ScriptTokenStream == null) return; if (f.ScriptTokenStream.isEmpty()) { f.ScriptTokenStream.addAll(_tokenSource._getScriptTokenStream()); } }',
        '    public void UpdateTokenInfo(Object... args) { if (args == null || args.length == 0 || !(args[0] instanceof TSqlFragment fragment)) return; AttachScriptTokenStream(fragment); if (args.length >= 3 && args[1] instanceof Number first && args[2] instanceof Number last) { fragment.UpdateTokenInfo(first.intValue(), last.intValue()); return; } if (args.length >= 2 && args[1] instanceof TSqlFragment child) { fragment.UpdateTokenInfo(child); return; } if (args.length >= 2) { int idx = ResolveTokenIndex(args[1]); if (idx >= 0) { if (fragment.firstTokenIndex == 0 || idx < fragment.firstTokenIndex) { fragment.firstTokenIndex = idx; } if (idx > fragment.lastTokenIndex) { fragment.lastTokenIndex = idx; } } } }',
        '    public void AddIdentifierToListWithCheck(Object... args) { if (args.length >= 2 && args[0] instanceof java.util.List list && args[1] != null) { if (args[1] instanceof java.util.Collection<?> c) { list.addAll(c); } else { list.add(args[1]); } } }',
        '    public SqlDataTypeOption ParseDataType180(Object s) { if (s == null) return SqlDataTypeOption.None; String str = s.toString(); for (SqlDataTypeOption opt : SqlDataTypeOption.values()) { if (opt.name().equalsIgnoreCase(str)) return opt; } return SqlDataTypeOption.None; }',
        '    public SqlDataTypeOption ParseDataType160(Object s) { return ParseDataType180(s); }',
        '    public SqlDataTypeOption ParseDataType100(Object s) { return ParseDataType180(s); }',
        '    public boolean IsXml(Object... args) { if (args.length > 0 && args[0] != null) { String str = (args[0] instanceof Identifier id && id.Value != null) ? id.Value : args[0].toString(); return "xml".equalsIgnoreCase(str); } return false; }',
        '    public boolean IsSys(Object... args) { if (args.length > 0 && args[0] != null) { String str = (args[0] instanceof Identifier id && id.Value != null) ? id.Value : args[0].toString(); return "sys".equalsIgnoreCase(str); } return false; }',
        '    public boolean IsTableReference(Object... args) { try { return LA(1) != LeftParenthesis; } catch (Exception e) { return true; } }',
        '    public boolean IsStatementIsNext(Object... args) { try { return (LA(1) != End || NextTokenMatches("CONVERSATION", 2)); } catch (Exception e) { return true; } }',
        '    public boolean NextTokenMatches(Object... args) { if (args.length == 0 || args[0] == null) return false; String keyword = args[0].toString(); int which = (args.length > 1 && args[1] instanceof Number n) ? n.intValue() : 1; try { Token tok = LT(which); return (tok != null && tok.getType() != EOF && keyword.equalsIgnoreCase(tok.getText())); } catch (Exception e) { return false; } }',
        '    public boolean NextTokenMatchesOneOf(Object... args) { try { Token tok = LT(1); if (tok == null || tok.getType() == EOF) return false; String text = tok.getText(); for (Object arg : args) { if (arg != null && text.equalsIgnoreCase(arg.toString())) return true; } } catch (Exception ignored) {} return false; }',
        '    public boolean ContainsVectorInLookahead(Object... args) { for (int i = 1; i <= 100; i++) { try { Token token = LT(i); if (token == null || token.getType() == EOF) break; if (token.getType() == Identifier && "VECTOR".equalsIgnoreCase(token.getText())) return true; } catch (Exception e) { break; } } return false; }',
        '    public boolean SkipGuessing(Object... args) throws antlr.RecognitionException { return false; }',
        '    public void SaveGuessing(Object... args) throws antlr.RecognitionException {}',
        '    public void RecoverAtStatementLevel(Object... args) { try { while (LA(1) != EOF && LA(1) != Semicolon && LA(1) != Go) { consume(); } if (LA(1) == Semicolon) { consume(); } } catch (Exception ignored) {} }',
        '    public void RecoverAtBatchLevel(Object... args) { try { while (LA(1) != EOF && LA(1) != Go) { consume(); } if (LA(1) == Go) { consume(); } } catch (Exception ignored) {} }',
        '    public IdentifierLiteral CreateIdentifierLiteral(String val, QuoteType qt) { IdentifierLiteral il = new IdentifierLiteral(); il.Value = val; il.QuoteType = qt; return il; }',
        '    public StringLiteral CreateStringLiteral(String val) { StringLiteral sl = new StringLiteral(); sl.Value = val; return sl; }',
        '    public TemporalClause CreateTemporalClause(TemporalClauseType clauseType) { TemporalClause tc = new TemporalClause(); tc.TemporalClauseType = clauseType; return tc; }',
        '    public IntegerLiteral CreateIntegerLiteral(String val) { IntegerLiteral il = new IntegerLiteral(); il.Value = val; return il; }',
        '    public IndexType CreateIndexType(IndexTypeKind kind) { IndexType it = new IndexType(); it.IndexTypeKind = kind; return it; }',
        '    public int tryParseIntVal(String s) { try { return java.lang.Integer.parseInt(s); } catch (Exception e) { return java.lang.Integer.MIN_VALUE; } }',
        '    public double tryParseDoubleVal(String s) { try { return java.lang.Double.parseDouble(s); } catch (Exception e) { return java.lang.Double.NaN; } }',
        '    public void SetField(Object target, String field, Object value) { if (target == null) return; try { target.getClass().getField(field).set(target, value); } catch (Exception ignored) {} }',
        '    public Object GetField(Object target, String field) { if (target == null) return null; try { return target.getClass().getField(field).get(target); } catch (Exception ignored) { return null; } }',
        '    public <T> T EnableDisableMatcher(Object... args) { if (args.length >= 3) { String txt = ""; if (args[0] instanceof Token t && t.getText() != null) txt = t.getText(); else if (args[0] instanceof Identifier id && id.Value != null) txt = id.Value; else if (args[0] != null) txt = args[0].toString(); if ("ENABLE".equalsIgnoreCase(txt)) return (T) args[1]; if ("DISABLE".equalsIgnoreCase(txt)) return (T) args[2]; return (T) args[1]; } return null; }',
        '    public <T> T AddBinaryExpression(Object... args) { if (args.length >= 3) { if (args[0] instanceof ScalarExpression left && args[1] instanceof ScalarExpression right && args[2] instanceof BinaryExpressionType bet) { BinaryExpression be = new BinaryExpression(); be.FirstExpression = left; be.SecondExpression = right; be.BinaryExpressionType = bet; return (T) be; } if (args[0] instanceof BooleanExpression left && args[1] instanceof BooleanExpression right && args[2] instanceof BooleanBinaryExpressionType bbet) { BooleanBinaryExpression bbe = new BooleanBinaryExpression(); bbe.FirstExpression = left; bbe.SecondExpression = right; bbe.BinaryExpressionType = bbet; return (T) bbe; } } return (T) (args.length > 0 ? args[0] : null); }',
        '    public boolean IsNextRuleSelectParenthesis(Object... args) { try { if (LA(1) == LeftParenthesis && LA(2) == Select) return true; int markSpot = mark(); boolean matches = false; try { consume(); int openParens = 1; while (true) { int la = LA(1); if (la == LeftParenthesis) { ++openParens; } else if (la == RightParenthesis) { --openParens; if (openParens == 0) break; } else if (la == EOF) { break; } else if (la == Join || la == Inner || la == Full || la == Cross || la == Outer || la == Union || la == Except || la == Intersect) { if (openParens == 1) { matches = true; break; } } consume(); } } finally { rewind(markSpot); } return matches; } catch (Exception e) { return false; } }',
        '    public boolean IsNextRuleBooleanParenthesis(Object... args) { try { if (LA(1) != LeftParenthesis) return false; boolean matches = false; int markSpot = mark(); try { consume(); int openParens = 1; int caseDepth = 0; int topmostSelect = 0; java.util.ArrayDeque<Integer> iifParenLevels = new java.util.ArrayDeque<>(); boolean pendingIIf = false; while (true) { boolean isIIfOpeningParen = pendingIIf && LA(1) == LeftParenthesis; pendingIIf = false; int la = LA(1); if (la == Identifier) { if (NextTokenMatches("IIF")) { pendingIIf = true; } else if (NextTokenMatches("REGEXP_LIKE")) { if (caseDepth == 0 && topmostSelect == 0 && iifParenLevels.isEmpty()) { matches = true; break; } } } else if (la == LeftParenthesis) { ++openParens; if (isIIfOpeningParen) { iifParenLevels.push(openParens); } } else if (la == RightParenthesis) { if (openParens == topmostSelect) { topmostSelect = 0; } if (!iifParenLevels.isEmpty() && iifParenLevels.peek() == openParens) { iifParenLevels.pop(); } --openParens; if (openParens == 0) break; } else if (la == EOF) { break; } else if (la == And || la == Or || la == Not || la == EqualsSign || la == GreaterThan || la == LessThan || la == Bang || la == MultiplyEquals || la == RightOuterJoin || la == Is || la == In || la == Like || la == Between || la == Contains || la == FreeText || la == Exists || la == TSEqual || la == Update) { if (caseDepth == 0 && topmostSelect == 0 && iifParenLevels.isEmpty()) { matches = true; break; } } else if (la == Case) { ++caseDepth; } else if (la == End) { --caseDepth; } else if (la == Select) { if (topmostSelect == 0) { topmostSelect = openParens; } } consume(); } } finally { rewind(markSpot); } return matches; } catch (Exception e) { return false; } }',
    ]
    for m in string_methods:
        h_lines.append(f'    public String {m}(Object... args) {{ if (args.length > 0 && args[0] != null) return args[0].toString(); return ""; }}')
    for m in helper_methods:
        h_lines.append(f'    public <T> T {m}(Object... args) {{ return null; }}')
    for m in boolean_methods:
        h_lines.append(f'    public boolean {m}(Object... args) {{ return false; }}')
    for m in void_methods:
        h_lines.append(f'    public void {m}(Object... args) {{}}')
    h_lines.append('}')
    (antlr_out / "TSql180ParserBaseInternalHelper.java").write_text("\n".join(h_lines), encoding="utf-8")
    (antlr_out / "TSql160ParserBaseInternalHelper.java").write_text('''package io.github.fupacat;
public class TSql160ParserBaseInternalHelper extends TSql180ParserBaseInternalHelper {
    public TSql160ParserBaseInternalHelper(boolean q) { super(q); }
    public TSql160ParserBaseInternalHelper(antlr.TokenStream l, int k) { super(l, k); }
}
''', encoding="utf-8")

    # 3f. Generate other *Helper classes
    all_helpers = set(re.findall(r'([A-Za-z0-9_]*Helper)\.Instance', grammar_src))
    all_helpers.update(re.findall(r'([A-Za-z0-9_]*Helper)\.[A-Za-z0-9_]', grammar_src))
    all_helpers.add('ExternalFileOption')
    
    for h in sorted(all_helpers):
        if h in {'TSql160ParserBaseInternalHelper', 'TSqlLexerBaseInternalHelper'}:
            continue
        code = f'''package io.github.fupacat;
import io.github.fupacat.ast.*;
public class {h} {{
    public static final {h} Instance = new {h}();
    @SuppressWarnings("unchecked")
    public <T> T ParseOption(Object... args) {{
        if ("PseudoColumnHelper".equals("{h}") && args.length > 0 && args[0] != null) {{
            String txt = (args[0] instanceof antlr.Token t && t.getText() != null) ? t.getText() : args[0].toString();
            if ("$node_id".equalsIgnoreCase(txt)) return (T) ColumnType.PseudoColumnGraphNodeId;
            if ("$edge_id".equalsIgnoreCase(txt)) return (T) ColumnType.PseudoColumnGraphEdgeId;
            if ("$from_id".equalsIgnoreCase(txt)) return (T) ColumnType.PseudoColumnGraphFromId;
            if ("$to_id".equalsIgnoreCase(txt)) return (T) ColumnType.PseudoColumnGraphToId;
            return (T) ColumnType.Regular;
        }}
        if ("ServiceBrokerOptionsHelper".equals("{h}") && args.length > 0 && args[0] != null) {{
            String txt = (args[0] instanceof antlr.Token t && t.getText() != null) ? t.getText() : args[0].toString();
            for (ServiceBrokerOption opt : ServiceBrokerOption.values()) {{
                if (opt.name().equalsIgnoreCase(txt)) return (T) opt;
            }}
            return (T) ServiceBrokerOption.DEFAULT;
        }}
        return null;
    }}
    public boolean TryParseOption(Object... args) {{ return false; }}
    public boolean RequiresEqualsSign(Object... args) {{ return false; }}
    public static boolean IsGraphPseudoColumn(Object... args) {{ return false; }}
    public static String Trim(Object obj) {{ return ""; }}
    public void AssignCopyColumnOptions(Object... args) {{}}
    public void AssignValueToCopyOption(Object... args) {{}}
    public boolean ValidateCopyCredential(Object... args) {{ return true; }}
    public void ValidateSecret(Object... args) {{}}
    public static void CheckXMLValidity(Object... args) {{}}
}}
'''
        (antlr_out / f"{h}.java").write_text(code, encoding="utf-8")

    # 3g. Non-AST Enums
    existing_ast_classes = {p.stem for p in ast_enum_dir.glob("*.java")}
    # Scan for enums that are NOT in existing AST classes and not helpers
    candidate_enums = set()
    for m in re.finditer(r'\b([A-Z][a-zA-Z0-9_]+)\.([A-Z][a-zA-Z0-9_]+)\b', grammar_src):
        typ, val = m.group(1), m.group(2)
        if typ not in existing_ast_classes and not typ.endswith('Helper') and typ not in skip:
            candidate_enums.add(typ)
    
    # Also check rule parameter types
    for m in re.finditer(r'\[\s*([A-Z][a-zA-Z0-9_]+)\s+[a-z][a-zA-Z0-9_]*\s*\]', grammar_src):
        typ = m.group(1)
        if typ not in existing_ast_classes and not typ.endswith('Helper') and typ not in skip:
            candidate_enums.add(typ)
            
    # Also specific enums known to be needed
    candidate_enums.update(['IndexAffectingStatement', 'ExpressionFlags', 'TokenKind', 'SqlVersionFlags'])
    
    for e_name in sorted(candidate_enums):
        if e_name in {'CodeGenerationSupporter', 'TSqlParserResource', 'TSqlParseErrorException',
                      'TSql160ParserBaseInternalHelper', 'TSqlLexerBaseInternalHelper',
                      'TSqlLexerBaseInternal', 'TSql160ParserBaseInternal', 'TSqlWhitespaceTokenFilter',
                      'TSqlParserToken', 'TSqlTokenType', 'ParseError', 'Enum'}:
            continue
        # Find constants
        consts = set(re.findall(r'\b' + e_name + r'\.([A-Za-z0-9_]+)\b', grammar_src))
        consts_list = ['DEFAULT'] + sorted(list(consts))
        enum_code = f'''package io.github.fupacat;

public enum {e_name} {{
    {", ".join(consts_list)};
    public int getValue() {{ return ordinal(); }}
}}
'''
        (antlr_out / f"{e_name}.java").write_text(enum_code, encoding="utf-8")

    # Generate SensitivityClassification
    (antlr_out / "SensitivityClassification.java").write_text(
        '''package io.github.fupacat;

public class SensitivityClassification {
    public enum Rank {
        DEFAULT, None, Low, Medium, High, Critical;
    }
    public static io.github.fupacat.ast.SensitivityClassificationOptionType GetOptionTypeByName(Object... args) {
        return io.github.fupacat.ast.SensitivityClassificationOptionType.DEFAULT;
    }
}
''', encoding="utf-8"
    )
    (antlr_out / "ExternalFileOption.java").write_text(
        '''package io.github.fupacat;

public class ExternalFileOption {
    public static void CheckXMLValidity(Object option, String optionName) {}
}
''', encoding="utf-8"
    )

    # 3h. SubDmlFlags
    (antlr_out / "SubDmlFlags.java").write_text(
        '''package io.github.fupacat;

public class SubDmlFlags {
    public static final int None = 0x0;
    public static final int InsideSubDml = 0x1;
    public static final int SelectNotForInsert = 0x2;
    public static final int MergeUsing = 0x4;
    public static final int UpdateDeleteFrom = 0x8;
}
''', encoding="utf-8"
    )

    # 4. Translate TSqlTokenTypes.g
    print("Step 4: Translating TSqlTokenTypes.g...")
    tt_raw = (core_dir / "src/main/resources/grammar/TSqlTokenTypes.g").read_text(encoding="utf-8")
    tt_clean = re.sub(r'header\s*\{[^}]*\}', '', tt_raw)
    tt_clean = re.sub(r'language\s*=\s*"CSharp";', 'language = "Java";', tt_clean)
    tt_clean = re.sub(r'namespace\s*=\s*"[^"]*";', '', tt_clean)
    tt_java = "header {\npackage io.github.fupacat;\n}\n\n" + tt_clean
    (antlr_out / "TSqlTokenTypes.g").write_text(tt_java, encoding="utf-8")

    # 5. Translate TSql180.g
    print("Step 5: Translating TSql180.g...")
    g_raw = (core_dir / "src/main/resources/grammar/TSql180.g").read_text(encoding="utf-8")
    
    g_text = re.sub(r'language\s*=\s*"CSharp";', 'language = "Java";', g_raw)
    g_text = re.sub(r'namespace\s*=\s*"[^"]*";', '', g_text)
    g_text = re.sub(r'\{\s*using\s+System[^}]*\}', '{}', g_text)

    header_block = '''header {
package io.github.fupacat;
import java.util.*;
import antlr.Token;
import io.github.fupacat.ast.*;
}

'''
    g_text = header_block + g_text
    
    # Class declarations
    g_text = re.sub(
        r'class\s+TSql180ParserInternal\s+extends\s+Parser\s*\("TSql180ParserBaseInternal"\)\s*;',
        'class TSql180ParserInternal extends Parser("TSql180ParserBaseInternalHelper");',
        g_text
    )
    g_text = re.sub(
        r'class\s+TSql180LexerInternal\s+extends\s+Lexer\s*\("TSqlLexerBaseInternal"\)\s*;',
        'class TSql180LexerInternal extends Lexer("TSqlLexerBaseInternalHelper");',
        g_text
    )
    
    # Clean pragma and header options
    g_text = re.sub(r'#pragma[^\n]*\n', '', g_text)
    g_text = re.sub(r'classHeaderPrefix\s*=\s*"[^"]*";', '', g_text)
    
    # Constructors
    g_text = re.sub(
        r'(?:public\s+)?TSql180ParserInternal\s*\(\s*bool(?:ean)?\s+initialQuotedIdentifiersOn\s*\)\s*:\s*base\s*\(\s*initialQuotedIdentifiersOn\s*\)\s*\{',
        'public TSql180ParserInternal(boolean initialQuotedIdentifiersOn) {\n        super(initialQuotedIdentifiersOn);',
        g_text
    )
    g_text = re.sub(
        r'(?:public\s+)?TSql180LexerInternal\s*\(\s*\)\s*:\s*this\s*\(\s*new\s+System\.IO\.StringReader\s*\(\s*String\.Empty\s*\)\s*\)\s*\{',
        'public TSql180LexerInternal() {\n        this(new java.io.StringReader(""));',
        g_text
    )
    
    # Factory calls: this.FragmentFactory.CreateFragment<Type>() -> new Type()
    g_text = re.sub(r'(?:this\.)?FragmentFactory\.CreateFragment<([A-Za-z0-9_]+)\s*>\(\)', r'new \1()', g_text)
    
    # Generic method calls
    g_text = re.sub(r'AddAndUpdateTokenInfo<[A-Za-z0-9_]+>\(', 'AddAndUpdateTokenInfo(', g_text)
    g_text = re.sub(r'\bnew\s+List<', 'new ArrayList<', g_text)
    g_text = re.sub(r'\bIList<', 'List<', g_text)

    # Object initializers
    g_text = g_text.replace(
        'new IdentifierLiteral { Value = vRankValue.getText(), QuoteType = QuoteType.NotQuoted }',
        'CreateIdentifierLiteral(vRankValue.getText(), QuoteType.NotQuoted)'
    )
    g_text = g_text.replace(
        'new StringLiteral { Value = BulkInsertStringOptionsHelper.Trim(vValue) }',
        'CreateStringLiteral(BulkInsertStringOptionsHelper.Trim(vValue))'
    )
    g_text = g_text.replace(
        'new IdentifierLiteral { Value = iValue.Value, QuoteType = QuoteType.NotQuoted }',
        'CreateIdentifierLiteral(iValue.Value, QuoteType.NotQuoted)'
    )
    g_text = re.sub(
        r'new\s+TemporalClause\s*\(\s*\)\s*\{\s*TemporalClauseType\s*=\s*clauseType\s*\}',
        'CreateTemporalClause(clauseType)',
        g_text
    )
    g_text = re.sub(
        r'new\s+IntegerLiteral\s*\(\s*\)\s*\{\s*Value\s*=\s*"-1"\s*\}',
        'CreateIntegerLiteral("-1")',
        g_text
    )
    g_text = re.sub(
        r'new\s+IndexType\s*\(\s*\)\s*\{\s*IndexTypeKind\s*=\s*IndexTypeKind\.([A-Za-z0-9_]+)\s*\}',
        r'CreateIndexType(IndexTypeKind.\1)',
        g_text
    )

    # Decimal, Int32, and TryParse parsing
    g_text = g_text.replace('0.75M', '0.75')
    g_text = g_text.replace('100.00M', '100.00')
    g_text = g_text.replace('0.25M', '0.25')
    g_text = g_text.replace('Int32 value;', 'int value = 0;')
    g_text = g_text.replace('Decimal dvalue;', 'double dvalue = 0.0;')
    g_text = g_text.replace('Int32 lowerRecursiveLimit;', 'int lowerRecursiveLimit = 0;')
    g_text = g_text.replace('Int32 upperRecursiveLimit;', 'int upperRecursiveLimit = 0;')
    g_text = re.sub(r'(?:int|Int32)\.TryParse\(\s*([^,]+),\s*NumberStyles\.Integer,\s*CultureInfo\.InvariantCulture,\s*(?:out\s+)?(\w+)\s*\)', r'(( \2 = tryParseIntVal(\1) ) != java.lang.Integer.MIN_VALUE)', g_text)
    g_text = re.sub(r'(?:double|Decimal)\.TryParse\(\s*([^,]+),\s*NumberStyles\.Float,\s*CultureInfo\.InvariantCulture,\s*(?:out\s+)?(\w+)\s*\)', r'(!java.lang.Double.isNaN( \2 = tryParseDoubleVal(\1) ))', g_text)
    g_text = re.sub(r'!\s*(?:int|Int32)\.TryParse\(\s*([^,]+),\s*(?:out\s+)?(?:int\s+)?(\w+)\s*\)', r'(tryParseIntVal(\1) == java.lang.Integer.MIN_VALUE)', g_text)
    g_text = re.sub(r'\b(?:int|Int32)\.Parse\(([^,)]+)[^)]*\)', r'java.lang.Integer.parseInt(\1)', g_text)
    g_text = re.sub(r'Enum\.TryParse\(.*?rank\)', 'true', g_text)
    g_text = re.sub(r'\bInt32\b', 'int', g_text)

    # String and Culture conversions
    g_text = g_text.replace('System.Text.StringBuilder', 'StringBuilder')
    g_text = g_text.replace('text.ToString()', 'text.toString()')
    g_text = g_text.replace('vStringBuilder.Append', 'vStringBuilder.append')
    g_text = g_text.replace('vStringBuilder.ToString()', 'vStringBuilder.toString()')
    # CodeGenerationSupporter comparisons
    g_text = re.sub(r'([A-Za-z0-9_.]+)\.ToUpper\(\s*CultureInfo\.InvariantCulture\s*\)', r'\1', g_text)
    g_text = re.sub(r'([A-Za-z0-9_.]+(?:\([^)]*\))?)\s*==\s*(CodeGenerationSupporter\.[A-Za-z0-9_]+)', r'\2.equalsIgnoreCase(\1)', g_text)
    g_text = re.sub(r'([A-Za-z0-9_.]+(?:\([^)]*\))?)\s*!=\s*(CodeGenerationSupporter\.[A-Za-z0-9_]+)', r'(!\2.equalsIgnoreCase(\1))', g_text)
    g_text = re.sub(r'(CodeGenerationSupporter\.[A-Za-z0-9_]+)\s*==\s*([A-Za-z0-9_.]+(?:\([^)]*\))?)', r'\1.equalsIgnoreCase(\2)', g_text)
    g_text = re.sub(r'(CodeGenerationSupporter\.[A-Za-z0-9_]+)\s*!=\s*([A-Za-z0-9_.]+(?:\([^)]*\))?)', r'(!\1.equalsIgnoreCase(\2))', g_text)
    g_text = re.sub(r'\.ToUpper\(\s*CultureInfo\.InvariantCulture\s*\)', '.toUpperCase()', g_text)
    g_text = re.sub(r'[sS]tring\.Format\(\s*culture,\s*"\{0\}\s+\{1\}",\s*([^,]+),\s*([^)]+)\)', r'(\1 + " " + \2)', g_text)
    g_text = re.sub(r'\.StartsWith\(\s*([^,]+),\s*StringComparison\.\w+\s*\)', r'.startsWith(\1)', g_text)
    g_text = g_text.replace('ExpressionFlags.MatchClauseAllowed != (expressionFlags & ExpressionFlags.MatchClauseAllowed)', 'expressionFlags != ExpressionFlags.MatchClauseAllowed')
    g_text = re.sub(r'String\.Equals\(\s*([^,]+),\s*([^,]+),\s*StringComparison\.OrdinalIgnoreCase\s*\)', r'((\1) != null && (\1).equalsIgnoreCase(\2))', g_text)
    g_text = re.sub(r'\b([A-Za-z0-9_]+(?:\([^)]*\))?(?:\.[A-Za-z0-9_]+(?:\([^)]*\))*)*)\.Equals\(\s*([^,]+),\s*StringComparison\.OrdinalIgnoreCase\s*\)', r'(\2.equalsIgnoreCase(\1))', g_text)


    # State and Enum initializations
    g_text = g_text.replace('Literal fragment1, fragment2;', 'Literal fragment1 = null, fragment2 = null;')
    g_text = g_text.replace('Literal fragment3, fragment4;', 'Literal fragment3 = null, fragment4 = null;')
    g_text = g_text.replace('DbccCommand command;', 'DbccCommand command = DbccCommand.DEFAULT;')
    g_text = g_text.replace('EventNotificationEventType eventTypeValue;', 'EventNotificationEventType eventTypeValue = EventNotificationEventType.DEFAULT;')
    g_text = g_text.replace('EventNotificationEventGroup eventGroupValue;', 'EventNotificationEventGroup eventGroupValue = EventNotificationEventGroup.DEFAULT;')
    g_text = g_text.replace('IToken token;', 'Token token = null;')
    g_text = g_text.replace('new MigrationState()', 'MigrationState.DEFAULT')
    g_text = g_text.replace('new RdaTableOption()', 'RdaTableOption.DEFAULT')
    g_text = g_text.replace('!vStatement.Clustered.Value', '!Boolean.TRUE.equals(vStatement.Clustered)')
    g_text = g_text.replace('vIsClustered.Value', 'Boolean.TRUE.equals(vIsClustered)')
    g_text = g_text.replace('vNonTransactedFileStreamAccess.Value', 'vNonTransactedFileStreamAccess')

    # Interface field accessors
    g_text = g_text.replace('vParent.Collation = vIdentifier;', 'SetField(vParent, "Collation", vIdentifier);')
    g_text = g_text.replace('vResult.IsMasked = true;', 'SetField(vResult, "IsMasked", true);')
    g_text = g_text.replace('vResult.MaskingFunction = vMaskingFunction;', 'SetField(vResult, "MaskingFunction", vMaskingFunction);')
    g_text = g_text.replace('vParent.Owner = vOwner;', 'SetField(vParent, "Owner", vOwner);')
    g_text = g_text.replace('vParent.FileStreamOn = vFragment;', 'SetField(vParent, "FileStreamOn", vFragment);')
    g_text = g_text.replace('vParent.FileStreamOn = vValue;', 'SetField(vParent, "FileStreamOn", vValue);')
    g_text = g_text.replace('vParent.EncryptionPassword = vPassword;', 'SetField(vParent, "EncryptionPassword", vPassword);')
    g_text = g_text.replace('vParent.DecryptionPassword = vPassword;', 'SetField(vParent, "DecryptionPassword", vPassword);')
    g_text = g_text.replace('if (vParent.EncryptionPassword != null)', 'if (GetField(vParent, "EncryptionPassword") != null)')
    g_text = g_text.replace('if (vParent.DecryptionPassword != null)', 'if (GetField(vParent, "DecryptionPassword") != null)')
    g_text = g_text.replace('if (vParent.FileStreamOn != null)', 'if (GetField(vParent, "FileStreamOn") != null)')

    # SensitivityClassification
    g_text = g_text.replace('SensitivityClassification.OptionType optionType', 'SensitivityClassificationOptionType optionType')
    g_text = g_text.replace('SensitivityClassification.OptionType.Undefined', 'SensitivityClassificationOptionType.DEFAULT')
    g_text = g_text.replace('SensitivityClassification.OptionType.Rank', 'SensitivityClassificationOptionType.Rank')
    g_text = g_text.replace('case SensitivityClassificationOptionType.DEFAULT:', 'case DEFAULT:')
    g_text = g_text.replace('case SensitivityClassificationOptionType.Rank:', 'case Rank:')
    g_text = g_text.replace('SensitivityClassification.Rank rank;', 'SensitivityClassification.Rank rank = SensitivityClassification.Rank.DEFAULT;')

    # Type keywords
    g_text = re.sub(r'\bbool\s*\?', 'Boolean', g_text)
    g_text = re.sub(r'\bint\s*\?', 'Integer', g_text)
    g_text = re.sub(r'\bboolean\s*\?', 'Boolean', g_text)
    g_text = re.sub(r'\bbool\b', 'boolean', g_text)
    g_text = re.sub(r'\bstring\b', 'String', g_text)
    g_text = re.sub(r'\bulong\b', 'long', g_text)
    g_text = re.sub(r'\bIToken\b', 'Token', g_text)
    
    # Nullable types in generics or variables: Identifier? -> Identifier
    g_text = re.sub(r'\b([A-Z][a-zA-Z0-9_]*)\?\s+([a-z][a-zA-Z0-9_]*)', r'\1 \2', g_text)
    g_text = re.sub(r'\b([A-Z][a-zA-Z0-9_]*)\?\b', r'\1', g_text)
    
    # Parameter qualifiers and ref binary expression
    g_text = re.sub(r'AddBinaryExpression\(\s*ref\s+([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\)', r'\1 = AddBinaryExpression(\1, \2, \3)', g_text)
    g_text = re.sub(r'\bref\s+', '', g_text)
    g_text = re.sub(r'\bout\s+', '', g_text)
    
    # Operators: is -> instanceof, as -> cast
    g_text = re.sub(r'\s+is\s+([A-Za-z0-9_]+)', r' instanceof \1', g_text)
    g_text = re.sub(r'([A-Za-z0-9_.]+)\s+as\s+([A-Za-z0-9_]+)', r'((\1) instanceof \2 ? (\2)(\1) : null)', g_text)

    
    # foreach (var x in y) -> for (var x : y)
    g_text = re.sub(r'foreach\s*\(\s*var\s+([A-Za-z0-9_]+)\s+in\s+([^)]+)\)', r'for (var \1 : \2)', g_text)
    
    # Null coalescing
    g_text = g_text.replace('GetFirstToken(vIndexType) ?? tIndex', '(GetFirstToken(vIndexType) != null ? GetFirstToken(vIndexType) : tIndex)')
    g_text = re.sub(r'([A-Za-z0-9_.]+)\s*\?\?\s*false', r'(\1 != null ? \1 : false)', g_text)
    g_text = re.sub(r'([A-Za-z0-9_.]+)\s*\?\?\s*""', r'(\1 != null ? \1 : "")', g_text)
    g_text = re.sub(r'\?\?\s*null', '', g_text)
    
    # Catch blocks and rethrow in ANTLR
    g_text = re.sub(r'catch\s*\[\s*([A-Za-z0-9_.]+)\s*\]', r'catch [ \1 exception ]', g_text)
    g_text = re.sub(r'\bthrow\s*;', 'throw exception;', g_text)
    
    # HasValue and Value
    g_text = re.sub(r'([A-Za-z0-9_.]+)\.HasValue\b', r'(\1 != null)', g_text)
    g_text = re.sub(r'([A-Za-z0-9_.]+)\.Value\s*==\s*true', r'Boolean.TRUE.equals(\1)', g_text)
    g_text = re.sub(r'([A-Za-z0-9_.]+)\.Value\s*==\s*false', r'Boolean.FALSE.equals(\1)', g_text)

    # Index accesses
    g_text = re.sub(r'vResult\.Columns\[0\]', 'vResult.Columns.get(0)', g_text)
    
    # List methods
    g_text = re.sub(r'\.Add\(', '.add(', g_text)
    g_text = re.sub(r'\.Count\b', '.size()', g_text)
    
    # (int) enum cast -> _ordinal(...)
    g_text = re.sub(r'\(int\)\s*([A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+)', r'_ordinal(\1)', g_text)
    
    # Bitwise |= on options
    g_text = re.sub(r'(\w+)\.RaiseErrorOptions\s*\|=\s*', r'\1.RaiseErrorOptions = ', g_text)
    g_text = re.sub(r'(\w+)\.Options\s*\|=\s*', r'\1.Options = ', g_text)
    g_text = re.sub(r'encountered\s*\|=\s*', 'encountered = ', g_text)
    g_text = re.sub(r'pathOptionEncountered\s*\|=\s*', 'pathOptionEncountered = ', g_text)

    (antlr_out / "TSql180.g").write_text(g_text, encoding="utf-8")

    # 6. Run ANTLR 2.7.7 Tool
    print("Step 6: Running ANTLR 2.7.7 Tool...")
    r1 = subprocess.run([java_cmd, "-cp", str(antlr_jar), "antlr.Tool", "-o", str(antlr_out), str(antlr_out / "TSqlTokenTypes.g")], stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
    if r1.returncode != 0:
        import sys
        sys.stderr.write(r1.stderr)
        sys.exit(r1.returncode)

    r2 = subprocess.run([java_cmd, "-cp", str(antlr_jar), "antlr.Tool", "-o", str(antlr_out), str(antlr_out / "TSql180.g")], stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
    if r2.returncode != 0:
        import sys
        sys.stderr.write(r2.stderr)
        sys.exit(r2.returncode)

    # 7. Post-generation patch on generated files
    print("Step 7: Applying post-generation patches...")
    p_file = antlr_out / "TSql180ParserInternal.java"
    p_src = p_file.read_text(encoding="utf-8")
    
    p_src = re.sub(r'(\w+)\.Value\.Length\b', r'\1.Value.length()', p_src)
    p_src = re.sub(r'(\w+)\.Length\b', r'\1.length()', p_src)
    p_src = re.sub(r'\.Add\(', '.add(', p_src)
    p_src = re.sub(r'\.Count\b', '.size()', p_src)
    p_src = re.sub(r'\(int\)\s*([A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+)', r'_ordinal(\1)', p_src)
    p_src = re.sub(r'\(int\)\s*vExternalTableOption\.OptionKind', r'_ordinal(vExternalTableOption.OptionKind)', p_src)
    p_src = re.sub(r'_tokenSource\.LastToken\.Offset', r'(_tokenSource != null && _tokenSource._getLastToken() != null ? _tokenSource._getLastToken().offset : 0)', p_src)
    p_src = re.sub(r'int\.TryParse\([^)]+\)', 'true', p_src)
    p_src = re.sub(r'\b(?:int|Int32)\.Parse\(([^,)]+)[^)]*\)', r'java.lang.Integer.parseInt(\1)', p_src)
    p_src = re.sub(r'\bint\.(InsideSubDml|SelectNotForInsert|UpdateDeleteFrom|MergeUsing|None)\b', r'SubDmlFlags.\1', p_src)
    p_src = re.sub(r'\bSubDmlFlags\s+subDmlFlags\b', 'int subDmlFlags', p_src)
    p_src = re.sub(r'\bcase\s+[A-Za-z0-9_]+\.([A-Za-z0-9_]+)\s*:', r'case \1:', p_src)
    p_src = re.sub(r'switch\s*\(\s*([A-Za-z0-9_.]+(?:\.[A-Za-z0-9_]+)*)\s*\)', r'if (\1 != null) switch (\1)', p_src)
    p_src = p_src.replace('encounteredOptions = encounteredOptions | vOption.Kind;', 'encounteredOptions = vOption.Kind;')
    p_src = p_src.replace('((encountered & PayloadOptionKinds.Role) != PayloadOptionKinds.Role)', '(encountered != PayloadOptionKinds.Role)')
    p_src = p_src.replace('(vResult.Options & SetOptions.QuotedIdentifier) == SetOptions.QuotedIdentifier', 'vResult.Options == SetOptions.QuotedIdentifier')
    p_src = p_src.replace('ExpressionFlags.ScalarSubqueriesDisallowed == (expressionFlags & ExpressionFlags.ScalarSubqueriesDisallowed)', 'expressionFlags == ExpressionFlags.ScalarSubqueriesDisallowed')
    p_src = p_src.replace('(encountered & vResult.OptionKind) != 0', 'encountered == vResult.OptionKind')
    p_src = p_src.replace('(encountered & JsonForClauseOptions.WithoutArrayWrapper) != 0', 'encountered == JsonForClauseOptions.WithoutArrayWrapper')
    p_src = p_src.replace('(encountered & JsonForClauseOptions.Root) != 0', 'encountered == JsonForClauseOptions.Root')
    p_src = re.sub(r'parameterNumber\.ToString\([^)]*\)', 'String.valueOf(parameterNumber)', p_src)
    p_src = p_src.replace('.ToString()', '.toString()')
    p_src = re.sub(r'(?<!java\.lang\.)\bInteger\.parseInt\(', 'java.lang.Integer.parseInt(', p_src)
    p_src = re.sub(r'(?<!java\.lang\.)\bInteger\.MIN_VALUE\b', 'java.lang.Integer.MIN_VALUE', p_src)
    p_src = re.sub(r'(?<!java\.lang\.)\bDouble\.isNaN\(', 'java.lang.Double.isNaN(', p_src)
    p_file.write_text(p_src, encoding="utf-8")
    
    l_file = antlr_out / "TSql180LexerInternal.java"
    l_src = l_file.read_text(encoding="utf-8")
    l_src = l_src.replace('text.Length', 'text.length()')
    l_src = l_src.replace('text.ToString()', 'text.toString()')
    l_src = re.sub(r'String\.Equals\([^,]+,\s*CodeGenerationSupporter\.DollarPartition[^)]*\)', 'false', l_src)
    l_file.write_text(l_src, encoding="utf-8")
    
    (antlr_out / "TSql160ParserInternal.java").write_text('''package io.github.fupacat;
public class TSql160ParserInternal extends TSql180ParserInternal {
    public TSql160ParserInternal(antlr.TokenStream lexer) { super(lexer); }
    public TSql160ParserInternal(antlr.TokenBuffer tokenBuf) { super(tokenBuf); }
    public TSql160ParserInternal(boolean q) { super(q); }
}
''', encoding="utf-8")
    (antlr_out / "TSql160LexerInternal.java").write_text('''package io.github.fupacat;
public class TSql160LexerInternal extends TSql180LexerInternal {
    public TSql160LexerInternal(java.io.Reader in) { super(in); }
    public TSql160LexerInternal(antlr.InputBuffer ib) { super(ib); }
    public TSql160LexerInternal(antlr.LexerSharedInputState state) { super(state); }
    public TSql160LexerInternal() { super(); }
}
''', encoding="utf-8")
    
    print("Generation complete! All sources generated in target/generated-sources.")

if __name__ == "__main__":
    main()
