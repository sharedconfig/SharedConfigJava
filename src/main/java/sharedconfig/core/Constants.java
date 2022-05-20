package sharedconfig.core;

/* package */ class Constants {
    public static class ApplicationInv {
        public static final String FileName = "application.inv.xml";

        public static class ApplicationElement {
            public static final String Name = "application";

            public static class BlocksElement {
                public static final String Name = "blocks";
            }

        }

        public static class VariablesElement {
            public static class VarElement {
                public static final String Name = "var";
                public static final String ApplicationNameAttribute = "a:n";
                public static final String ApplicationVersionAttribute = "a:v";
                public static final String BlockNameAttribute = "b:n";
                public static final String BlockVersionAttribute = "b:v";
            }
        }
    }

    public static class Prepared {
        public static String FileName = "prepared.xml";
        public static String RootTagName = "changes";
        public static String ChangesetTagName = "item";

    }

    public static class PreparedVars {
        public static String FileName = "prepared.vars.xml";
        public static class VariablesElement {
            public static String TagName = "variables";
            public static class VarElement {
                public static String TagName = "var";
            }
        }
    }

}
