package io.github.fupacat;

import java.nio.file.Path;

public class AstCodeGenerator {

    private static final String PACKAGE_NAME = "io.github.fupacat.ast";

    // TRACEABILITY_MAPPING: REQ-002, REQ-003, REQ-004, REQ-005
    public int generate(AstModel model, Path outputDirectory) throws Exception {
        int generatedCount = 0;

        java.util.Set<String> classNames = new java.util.HashSet<>();
        for (AstModel.ClassDef c : model.classes) {
            classNames.add(c.name);
        }

        // 1. Generate TSqlFragment base class
        generatedCount += _generateBaseFragment(outputDirectory);

        // 2. Generate enums
        generatedCount += _generateEnums(model, outputDirectory);

        // 3. Generate interfaces
        generatedCount += _generateInterfaces(model, outputDirectory);

        // 4. Generate all AST node classes
        generatedCount += _generateClasses(model, classNames, outputDirectory);

        // 5. Generate TSqlFragmentVisitor interface (REQ-003)
        generatedCount += _generateVisitor(model, outputDirectory);

        // 6. Generate TSqlConcreteFragmentVisitor (REQ-004)
        generatedCount += _generateConcreteVisitor(model, outputDirectory);

        return generatedCount;
    }

    // TRACEABILITY_MAPPING: REQ-002
    private int _generateBaseFragment(Path outputDirectory) throws Exception {
        com.squareup.javapoet.TypeSpec fragmentSpec = com.squareup.javapoet.TypeSpec.classBuilder("TSqlFragment")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.ABSTRACT)
                .addSuperinterface(java.io.Serializable.class)
                .addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.TypeName.INT, "firstTokenIndex", javax.lang.model.element.Modifier.PUBLIC).build())
                .addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.TypeName.INT, "lastTokenIndex", javax.lang.model.element.Modifier.PUBLIC).build())
                .addField(com.squareup.javapoet.FieldSpec.builder(
                        com.squareup.javapoet.ClassName.get(java.util.List.class), "ScriptTokenStream", javax.lang.model.element.Modifier.PUBLIC)
                        .initializer("new $T<>()", java.util.ArrayList.class).build())
                .addField(com.squareup.javapoet.FieldSpec.builder(String.class, "Value", javax.lang.model.element.Modifier.PUBLIC).build())
                .addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("UpdateTokenInfo")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .addParameter(int.class, "first")
                        .addParameter(int.class, "last")
                        .addStatement("this.firstTokenIndex = first")
                        .addStatement("this.lastTokenIndex = last")
                        .build())
                .addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("UpdateTokenInfo")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .addParameter(Object.class, "other")
                        .build())
                .addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("SetUnquotedIdentifier")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .addParameter(String.class, "identifier")
                        .addStatement("this.Value = identifier")
                        .build())
                .addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("SetIdentifier")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .addParameter(String.class, "identifier")
                        .addStatement("this.Value = identifier")
                        .build())
                .addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("accept")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.ABSTRACT)
                        .addParameter(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "TSqlFragmentVisitor"), "visitor")
                        .build())
                .addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("acceptChildren")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .addParameter(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "TSqlFragmentVisitor"), "visitor")
                        .build())
                .build();

        com.squareup.javapoet.JavaFile.builder(PACKAGE_NAME, fragmentSpec).build().writeTo(outputDirectory);
        return 1;
    }

    // TRACEABILITY_MAPPING: REQ-002
    private int _generateEnums(AstModel model, Path outputDirectory) throws Exception {
        int count = 0;
        for (String enumName : model.enums) {
            String cleanName = enumName.trim();
            if (cleanName.contains(".")) {
                cleanName = cleanName.replace(".", "");
            }
            com.squareup.javapoet.TypeSpec enumSpec = com.squareup.javapoet.TypeSpec.enumBuilder(cleanName)
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                    .addEnumConstant("DEFAULT")
                    .build();
            com.squareup.javapoet.JavaFile.builder(PACKAGE_NAME, enumSpec).build().writeTo(outputDirectory);
            count++;
        }
        return count;
    }

    // TRACEABILITY_MAPPING: REQ-002
    private int _generateInterfaces(AstModel model, Path outputDirectory) throws Exception {
        int count = 0;
        for (String ifName : model.interfaces) {
            com.squareup.javapoet.TypeSpec ifSpec = com.squareup.javapoet.TypeSpec.interfaceBuilder(ifName)
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                    .build();
            com.squareup.javapoet.JavaFile.builder(PACKAGE_NAME, ifSpec).build().writeTo(outputDirectory);
            count++;
        }
        return count;
    }

    // TRACEABILITY_MAPPING: REQ-002
    private int _generateClasses(AstModel model, java.util.Set<String> classNames, Path outputDirectory) throws Exception {
        int count = 0;
        for (AstModel.ClassDef c : model.classes) {
            com.squareup.javapoet.TypeSpec.Builder classBuilder = com.squareup.javapoet.TypeSpec.classBuilder(c.name)
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC);

            if (c.isAbstract) {
                classBuilder.addModifiers(javax.lang.model.element.Modifier.ABSTRACT);
            }

            if (c.summary != null && !c.summary.isEmpty()) {
                classBuilder.addJavadoc("$L\n", c.summary);
            }

            if (c.base != null && !c.base.isEmpty()) {
                classBuilder.superclass(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, c.base));
            } else {
                classBuilder.superclass(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "TSqlFragment"));
            }

            for (String iface : c.interfaces) {
                classBuilder.addSuperinterface(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, iface));
            }

            if ("SchemaObjectName".equals(c.name)) {
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "Identifier"), "SchemaIdentifier", javax.lang.model.element.Modifier.PUBLIC).build());
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "Identifier"), "BaseIdentifier", javax.lang.model.element.Modifier.PUBLIC).build());
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "Identifier"), "DatabaseIdentifier", javax.lang.model.element.Modifier.PUBLIC).build());
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "Identifier"), "ServerIdentifier", javax.lang.model.element.Modifier.PUBLIC).build());
            }

            if ("MultiPartIdentifier".equals(c.name)) {
                classBuilder.addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("size")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .returns(int.class)
                        .addStatement("return Identifiers != null ? Identifiers.size() : 0")
                        .build());
                classBuilder.addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("Count")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .returns(int.class)
                        .addStatement("return size()")
                        .build());
            }

            if ("Identifier".equals(c.name)) {
                classBuilder.addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("SetUnquotedIdentifier")
                        .addAnnotation(Override.class)
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .addParameter(String.class, "identifier")
                        .addStatement("this.Value = identifier")
                        .addStatement("super.Value = identifier")
                        .build());
                classBuilder.addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("SetIdentifier")
                        .addAnnotation(Override.class)
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .addParameter(String.class, "identifier")
                        .addStatement("this.Value = identifier")
                        .addStatement("super.Value = identifier")
                        .build());
            }

            if (c.interfaces.contains("IAuthorization")) {
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "Identifier"), "Owner", javax.lang.model.element.Modifier.PUBLIC).build());
            }
            if (c.interfaces.contains("ICollationSetter") || "ColumnDefinitionBase".equals(c.name) || "PrimaryExpression".equals(c.name)) {
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "Identifier"), "Collation", javax.lang.model.element.Modifier.PUBLIC).build());
            }
            if (c.interfaces.contains("IDataMaskingSetter") || "ColumnDefinition".equals(c.name) || "AlterTableAlterColumnStatement".equals(c.name)) {
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(Boolean.class), "IsMasked", javax.lang.model.element.Modifier.PUBLIC).build());
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "StringLiteral"), "MaskingFunction", javax.lang.model.element.Modifier.PUBLIC).build());
            }
            if (c.interfaces.contains("IFileStreamSpecifier") || "CreateTableStatement".equals(c.name)) {
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "IdentifierOrValueExpression"), "FileStreamOn", javax.lang.model.element.Modifier.PUBLIC).build());
            }
            if (c.interfaces.contains("IPasswordChangeOption") || "CertificateStatementBase".equals(c.name) || "CreateCertificateStatement".equals(c.name)) {
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "Literal"), "EncryptionPassword", javax.lang.model.element.Modifier.PUBLIC).build());
                classBuilder.addField(com.squareup.javapoet.FieldSpec.builder(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "Literal"), "DecryptionPassword", javax.lang.model.element.Modifier.PUBLIC).build());
            }

            // Members
            for (AstModel.MemberDef m : c.members) {
                com.squareup.javapoet.TypeName fieldType = _resolveType(m.type, m.collection);
                com.squareup.javapoet.FieldSpec.Builder fieldBuilder = com.squareup.javapoet.FieldSpec.builder(
                        fieldType, m.name, javax.lang.model.element.Modifier.PUBLIC);

                if (m.summary != null && !m.summary.isEmpty()) {
                    fieldBuilder.addJavadoc("$L\n", m.summary);
                }

                if (m.collection) {
                    fieldBuilder.initializer("new $T<>()", java.util.ArrayList.class);
                }

                classBuilder.addField(fieldBuilder.build());
            }

            // accept method
            com.squareup.javapoet.MethodSpec.Builder acceptBuilder = com.squareup.javapoet.MethodSpec.methodBuilder("accept")
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "TSqlFragmentVisitor"), "visitor");

            if (!c.isAbstract) {
                acceptBuilder.beginControlFlow("if (visitor != null)")
                        .addStatement("visitor.visit(this)")
                        .endControlFlow();
            }
            classBuilder.addMethod(acceptBuilder.build());

            // acceptChildren method
            com.squareup.javapoet.MethodSpec.Builder acceptChildrenBuilder = com.squareup.javapoet.MethodSpec.methodBuilder("acceptChildren")
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "TSqlFragmentVisitor"), "visitor");

            if (c.base != null && !c.base.isEmpty()) {
                acceptChildrenBuilder.addStatement("super.acceptChildren(visitor)");
            }

            for (AstModel.MemberDef m : c.members) {
                String cleanType = m.type.trim();
                if (cleanType.endsWith("?")) {
                    cleanType = cleanType.substring(0, cleanType.length() - 1);
                }
                if (classNames.contains(cleanType)) {
                    if (m.collection) {
                        acceptChildrenBuilder.beginControlFlow("for (var item : this.$L)", m.name)
                                .beginControlFlow("if (item != null)")
                                .addStatement("item.accept(visitor)")
                                .endControlFlow()
                                .endControlFlow();
                    } else {
                        acceptChildrenBuilder.beginControlFlow("if (this.$L != null)", m.name)
                                .addStatement("this.$L.accept(visitor)", m.name)
                                .endControlFlow();
                    }
                }
            }
            classBuilder.addMethod(acceptChildrenBuilder.build());

            com.squareup.javapoet.JavaFile.builder(PACKAGE_NAME, classBuilder.build()).build().writeTo(outputDirectory);
            count++;
        }
        return count;
    }

    // TRACEABILITY_MAPPING: REQ-003
    private int _generateVisitor(AstModel model, Path outputDirectory) throws Exception {
        com.squareup.javapoet.TypeSpec.Builder visitorBuilder = com.squareup.javapoet.TypeSpec.interfaceBuilder("TSqlFragmentVisitor")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC);

        for (AstModel.ClassDef c : model.classes) {
            if (!c.isAbstract) {
                com.squareup.javapoet.ClassName nodeType = com.squareup.javapoet.ClassName.get(PACKAGE_NAME, c.name);
                visitorBuilder.addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("visit")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.DEFAULT)
                        .addParameter(nodeType, "node")
                        .build());
                visitorBuilder.addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("explicitVisit")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.DEFAULT)
                        .addParameter(nodeType, "node")
                        .build());
            }
        }

        com.squareup.javapoet.JavaFile.builder(PACKAGE_NAME, visitorBuilder.build()).build().writeTo(outputDirectory);
        return 1;
    }

    // TRACEABILITY_MAPPING: REQ-004
    private int _generateConcreteVisitor(AstModel model, Path outputDirectory) throws Exception {
        com.squareup.javapoet.TypeSpec.Builder concreteBuilder = com.squareup.javapoet.TypeSpec.classBuilder("TSqlConcreteFragmentVisitor")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .addSuperinterface(com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "TSqlFragmentVisitor"));

        for (AstModel.ClassDef c : model.classes) {
            if (!c.isAbstract) {
                com.squareup.javapoet.ClassName nodeType = com.squareup.javapoet.ClassName.get(PACKAGE_NAME, c.name);
                concreteBuilder.addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("visit")
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(nodeType, "node")
                        .beginControlFlow("if (node != null)")
                        .addStatement("node.acceptChildren(this)")
                        .endControlFlow()
                        .build());
            }
        }

        com.squareup.javapoet.JavaFile.builder(PACKAGE_NAME, concreteBuilder.build()).build().writeTo(outputDirectory);
        return 1;
    }

    // TRACEABILITY_MAPPING: REQ-002
    private com.squareup.javapoet.TypeName _resolveType(String rawType, boolean isCollection) {
        String clean = rawType.trim();
        boolean isNullable = clean.endsWith("?");
        if (isNullable) {
            clean = clean.substring(0, clean.length() - 1);
        }

        com.squareup.javapoet.TypeName baseType;
        if (clean.equals("bool") || clean.equals("Boolean")) {
            baseType = (isCollection || isNullable) ? com.squareup.javapoet.ClassName.get(Boolean.class) : com.squareup.javapoet.TypeName.BOOLEAN;
        } else if (clean.equals("int")) {
            baseType = (isCollection || isNullable) ? com.squareup.javapoet.ClassName.get(Integer.class) : com.squareup.javapoet.TypeName.INT;
        } else if (clean.equals("string")) {
            baseType = com.squareup.javapoet.ClassName.get(String.class);
        } else if (clean.equals("SensitivityClassification.OptionType")) {
            baseType = com.squareup.javapoet.ClassName.get(PACKAGE_NAME, "SensitivityClassificationOptionType");
        } else {
            baseType = com.squareup.javapoet.ClassName.get(PACKAGE_NAME, clean);
        }

        if (isCollection) {
            return com.squareup.javapoet.ParameterizedTypeName.get(
                    com.squareup.javapoet.ClassName.get(java.util.List.class),
                    baseType.isPrimitive() ? baseType.box() : baseType
            );
        }
        return baseType;
    }
}
