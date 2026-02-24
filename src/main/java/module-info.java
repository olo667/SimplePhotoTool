module com.example.simplephototool {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive javafx.base;
    requires transitive javafx.media;
    requires java.desktop;
    requires jdk.httpserver;

    opens com.example.simplephototool to javafx.fxml;
    exports com.example.simplephototool;
}