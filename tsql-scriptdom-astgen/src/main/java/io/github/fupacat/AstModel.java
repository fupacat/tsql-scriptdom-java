package io.github.fupacat;

public class AstModel {
    public static class MemberDef {
        public final String name;
        public final String type;
        public final boolean collection;
        public final String summary;

        public MemberDef(String name, String type, boolean collection, String summary) {
            this.name = name;
            this.type = type;
            this.collection = collection;
            this.summary = summary;
        }
    }

    public static class ClassDef {
        public final String name;
        public final String base;
        public final boolean isAbstract;
        public final String summary;
        public final java.util.List<MemberDef> members = new java.util.ArrayList<>();
        public final java.util.List<String> interfaces = new java.util.ArrayList<>();

        public ClassDef(String name, String base, boolean isAbstract, String summary) {
            this.name = name;
            this.base = base;
            this.isAbstract = isAbstract;
            this.summary = summary;
        }
    }

    public final java.util.List<ClassDef> classes = new java.util.ArrayList<>();
    public final java.util.Set<String> enums = new java.util.TreeSet<>();
    public final java.util.Set<String> interfaces = new java.util.TreeSet<>();
}
