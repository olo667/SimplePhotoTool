module com.example.simplephototool {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive javafx.base;
    requires java.desktop;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.bytedeco.javacv;

    opens com.example.simplephototool to javafx.fxml;
    exports com.example.simplephototool;
}