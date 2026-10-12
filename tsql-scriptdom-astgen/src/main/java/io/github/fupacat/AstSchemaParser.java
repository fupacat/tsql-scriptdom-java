package io.github.fupacat;

import java.io.InputStream;
import javax.xml.XMLConstants;

public class AstSchemaParser {

    // TRACEABILITY_MAPPING: REQ-001
    public AstModel parse(InputStream inputStream) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        AstModel model = new AstModel();

        org.w3c.dom.NodeList classNodes = doc.getElementsByTagName("Class");
        for (int i = 0; i < classNodes.getLength(); i++) {
            org.w3c.dom.Element classElem = (org.w3c.dom.Element) classNodes.item(i);
            String name = classElem.getAttribute("Name").trim();
            String base = classElem.hasAttribute("Base") ? classElem.getAttribute("Base").trim() : null;
            if (base != null && base.isEmpty()) {
                base = null;
            }
            boolean isAbstract = "true".equalsIgnoreCase(classElem.getAttribute("Abstract"));
            String summary = classElem.getAttribute("Summary");

            AstModel.ClassDef classDef = new AstModel.ClassDef(name, base, isAbstract, summary);

            org.w3c.dom.NodeList memberNodes = classElem.getElementsByTagName("Member");
            for (int j = 0; j < memberNodes.getLength(); j++) {
                org.w3c.dom.Element memberElem = (org.w3c.dom.Element) memberNodes.item(j);
                String mName = memberElem.getAttribute("Name").trim();
                String mType = memberElem.getAttribute("Type").trim();
                boolean collection = "true".equalsIgnoreCase(memberElem.getAttribute("Collection"));
                String mSummary = memberElem.getAttribute("Summary");

                classDef.members.add(new AstModel.MemberDef(mName, mType, collection, mSummary));
            }

            org.w3c.dom.NodeList implNodes = classElem.getElementsByTagName("Implements");
            for (int j = 0; j < implNodes.getLength(); j++) {
                org.w3c.dom.Element implElem = (org.w3c.dom.Element) implNodes.item(j);
                String iface = implElem.getAttribute("Interface").trim();
                if (!iface.isEmpty()) {
                    classDef.interfaces.add(iface);
                }
            }

            model.classes.add(classDef);
        }

        org.w3c.dom.NodeList interfaceNodes = doc.getElementsByTagName("Interface");
        for (int i = 0; i < interfaceNodes.getLength(); i++) {
            org.w3c.dom.Element ifElem = (org.w3c.dom.Element) interfaceNodes.item(i);
            String ifName = ifElem.getAttribute("Name").trim();
            if (!ifName.isEmpty()) {
                model.interfaces.add(ifName);
            }
        }

        java.util.Set<String> classNames = new java.util.HashSet<>();
        for (AstModel.ClassDef c : model.classes) {
            classNames.add(c.name);
        }

        for (AstModel.ClassDef c : model.classes) {
            for (AstModel.MemberDef m : c.members) {
                String typeName = m.type;
                if (typeName.endsWith("?")) {
                    typeName = typeName.substring(0, typeName.length() - 1);
                }
                if (!classNames.contains(typeName)
                        && !model.interfaces.contains(typeName)
                        && !typeName.equals("bool")
                        && !typeName.equals("Boolean")
                        && !typeName.equals("int")
                        && !typeName.equals("string")) {
                    model.enums.add(typeName);
                }
            }
        }

        return model;
    }
}
