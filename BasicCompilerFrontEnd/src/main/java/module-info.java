module com.compiler.frontend {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.compiler.frontend to javafx.fxml;
    exports com.compiler.frontend;
}