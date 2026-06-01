module com.pcwarehouse {
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.flywaydb.core;

    exports com.pcwarehouse;
    exports com.pcwarehouse.controller;
    exports com.pcwarehouse.db;

    opens com.pcwarehouse to javafx.fxml;
    opens com.pcwarehouse.controller to javafx.fxml;
}
