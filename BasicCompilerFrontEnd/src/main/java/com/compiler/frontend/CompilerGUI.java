package com.compiler.frontend;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CompilerGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create the main layout
        BorderPane root = new BorderPane();

        // Left panel with buttons
        VBox leftPanel = createLeftPanel();
        root.setLeft(leftPanel);

        // Right panel with text areas
        VBox rightPanel = createRightPanel();
        root.setCenter(rightPanel);

        // Create the scene
        Scene scene = new Scene(root, 800, 600);

        // Load the CSS stylesheet
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        primaryStage.setTitle("Compiler GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setAlignment(Pos.CENTER);

        // Create buttons
        Button openFileBtn = new Button("Open File");
        Button lexicalBtn = new Button("Lexical Analysis");
        Button syntaxBtn = new Button("Syntax Analysis");
        Button semanticBtn = new Button("Semantic Analysis");
        Button clearBtn = new Button("Clear");

        // Make buttons grow to fill the width
        openFileBtn.setMaxWidth(Double.MAX_VALUE);
        lexicalBtn.setMaxWidth(Double.MAX_VALUE);
        syntaxBtn.setMaxWidth(Double.MAX_VALUE);
        semanticBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);

        // Add buttons to the panel
        leftPanel.getChildren().addAll(openFileBtn, lexicalBtn, syntaxBtn, semanticBtn, clearBtn);

        return leftPanel;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));

        // First text box using a TitledPane
        TitledPane resultPane = new TitledPane();
        resultPane.setText("Result:");
        resultPane.setCollapsible(false);

        TextArea resultTextArea = new TextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setWrapText(true);
        resultPane.setContent(resultTextArea);

        // Second text area
        TextArea mainTextArea = new TextArea();
        mainTextArea.setWrapText(true);

        // Add both components to the right panel
        rightPanel.getChildren().addAll(resultPane, mainTextArea);

        // Set the vertical growth constraints
        VBox.setVgrow(resultPane, Priority.NEVER);
        VBox.setVgrow(mainTextArea, Priority.ALWAYS);

        // Set the first TitledPane to take 1/6 of the height
        resultPane.prefHeightProperty().bind(rightPanel.heightProperty().multiply(0.166));

        return rightPanel;
    }

    public static void main(String[] args) {
        launch(args);
    }
}