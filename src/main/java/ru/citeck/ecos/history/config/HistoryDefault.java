package ru.citeck.ecos.history.config;

public class HistoryDefault {

    private HistoryDefault() {

    }

    public class Event {

        private Event() {
        }

        public static final String HOST = "localhost";
        public static final int PORT = 0;
        public static final String USERNAME = "";
        public static final String PASSWORD = "";

    }

    public class Alfresco {
        private Alfresco() {
        }

        public static final String TENANT_ID = "local-ecos";
    }

}
