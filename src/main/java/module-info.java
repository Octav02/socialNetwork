module com.projects.socialnetwork {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires spring.security.crypto;

    opens com.projects.socialnetwork to javafx.fxml;
    exports com.projects.socialnetwork;
    exports com.projects.socialnetwork.controllers;
    opens com.projects.socialnetwork.controllers to javafx.fxml;

    exports com.projects.socialnetwork.models;
    opens com.projects.socialnetwork.models to javafx.base;

    exports com.projects.socialnetwork.dtos;
    opens com.projects.socialnetwork.dtos to javafx.base;
}