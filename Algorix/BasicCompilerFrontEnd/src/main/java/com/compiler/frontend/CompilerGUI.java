package com.compiler.frontend;

import Controller.FileChooser;
import Model.LexicalAnalysis;
import Model.SemanticAnalysis;
import Model.SyntaxAnalysis;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CompilerGUI extends Application {    private TextArea mainTextArea;
    private TextArea resultTextArea;

    @Override
    public void start(Stage primaryStage) {
        // Main layout
        BorderPane root = new BorderPane();

        Label title = new Label("Algorix Mini Java Compiler");
        title.getStyleClass().add("app-title");        // This applies the CSS above
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        // FIX 1: Removed inline setStyle call.
        root.setTop(title);

        // Left panel (buttons)
        VBox leftPanel = createLeftPanel();
        root.setLeft(leftPanel);

        // Right panel (text areas)
        VBox rightPanel = createRightPanel();
        root.setCenter(rightPanel);

        // Scene & CSS
        Scene scene = new Scene(root, 1100, 720);  // a bit larger looks better with title
        scene.getStylesheets().add(
                getClass().getResource("/dark-theme.css").toExternalForm()
        );

        primaryStage.setTitle("Algorix Mini Java Compiler");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setAlignment(Pos.CENTER);
        // FIX: Add left-panel class to VBox here for background/padding (if desired)
        // leftPanel.getStyleClass().add("left-panel"); // (Recommended, but keeping previous functionality for now)

        // Create buttons
        Button openFileBtn = new Button("Open File");
        openFileBtn.getStyleClass().add("primary-action"); // Ensure primary action button is prominent
        Button lexicalBtn = new Button("Lexical Analysis");
        Button syntaxBtn = new Button("Syntax Analysis");
        Button semanticBtn = new Button("Semantic Analysis");
        Button clearBtn = new Button("Clear");

        lexicalBtn.setDisable(true); // disabled initially
        syntaxBtn.setDisable(true); // disabled initially
        semanticBtn.setDisable(true); // disabled initially

        clearBtn.setOnAction(event -> { // Clears the source code and result areas
            mainTextArea.clear();
            resultTextArea.clear();
            lexicalBtn.setDisable(true);
            syntaxBtn.setDisable(true);
            semanticBtn.setDisable(true);
        });

        // Open File button - enables Lexical Analysis
        openFileBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            String fileContent = fileChooser.openFile();

            if (fileContent != null && !fileContent.isEmpty()) {
                mainTextArea.setText(fileContent);
                lexicalBtn.setDisable(false); // Enable Lexical Analysis
                syntaxBtn.setDisable(true); // Reset Syntax Analysis
                semanticBtn.setDisable(true); // Reset Semantic Analysis
                resultTextArea.clear();
            }
        });

        // Lexical Analysis button - enables Syntax Analysis only on PASS
        lexicalBtn.setOnAction(event -> {
            String sourceCode = mainTextArea.getText();
            if (sourceCode == null || sourceCode.trim().isEmpty()) {
                resultTextArea.setText("Error: Please load a file or enter source code first.");
                syntaxBtn.setDisable(true);
            } else {
                LexicalAnalysis lexical = new LexicalAnalysis();
                String result = lexical.analyze(sourceCode);
                resultTextArea.setText(result);

                // Only enable Syntax Analysis if Lexical Analysis PASSED
                if (lexical.isPassed()) {
                    syntaxBtn.setDisable(false); // Enable Syntax Analysis
                    lexicalBtn.setDisable(true); // Disable Lexical Analysis after pass
                } else {
                    syntaxBtn.setDisable(true); // Keep Syntax disabled
                }
            }
        });

        // Syntax Analysis button - enables Semantic Analysis only on PASS
        syntaxBtn.setOnAction(event -> {
            String sourceCode = mainTextArea.getText();
            if (sourceCode == null || sourceCode.trim().isEmpty()) {
                resultTextArea.setText("Error: Please load a file or enter source code first.");
                semanticBtn.setDisable(true);
            } else {
                SyntaxAnalysis syntax = new SyntaxAnalysis();
                String result = syntax.analyze(sourceCode);
                resultTextArea.setText(result);

                // Only enable Semantic Analysis if Syntax Analysis PASSED
                if (syntax.isPassed()) {
                    semanticBtn.setDisable(false); // Enable Semantic Analysis
                    syntaxBtn.setDisable(true); // Disable Syntax Analysis after pass
                } else {
                    semanticBtn.setDisable(true); // Keep Semantic disabled
                }
            }
        });

        // Semantic Analysis button handler
        semanticBtn.setOnAction(event -> {
            String sourceCode = mainTextArea.getText();
            if (sourceCode == null || sourceCode.trim().isEmpty()) {
                resultTextArea.setText("Error: Please load a file or enter source code first.");
            } else {
                SemanticAnalysis semantic = new SemanticAnalysis();
                String result = semantic.analyze(sourceCode);
                resultTextArea.setText(result);
                // Only disable Semantic Analysis if it PASSED
                if (semantic.isPassed()) {
                    semanticBtn.setDisable(true); // Disable only on pass
                }
                // If it fails, button remains enabled for retry
            }
        });


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
        resultPane.setText("Compiler Output"); // Changed to the more professional title
        resultPane.setCollapsible(false);
        resultPane.getStyleClass().add("result-pane");

        resultTextArea = new TextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setWrapText(true);
        // FIX 2: Add CSS class to resultTextArea for targetting
        resultTextArea.getStyleClass().add("compiler-result-area");
        resultPane.setContent(resultTextArea);

        // Second text area with line numbers
        mainTextArea = new TextArea();
        mainTextArea.setWrapText(true);
        // FIX 3: REMOVED INLINE STYLE.

        // Create line number area
        TextArea lineNumbers = new TextArea();
        lineNumbers.setEditable(false);
        lineNumbers.setWrapText(false);
        lineNumbers.setPrefWidth(55); // Slightly wider
        lineNumbers.getStyleClass().add("line-numbers"); // Add CSS class
        // FIX 4: REMOVED INLINE STYLE.

        // Sync line numbers with main text area
        mainTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int lines = newVal.split("\n", -1).length;
            StringBuilder lineNumbersText = new StringBuilder();
            for (int i = 1; i <= lines; i++) {
                lineNumbersText.append(i).append("\n");
            }
            lineNumbers.setText(lineNumbersText.toString());
        });

        // Sync scrolling between text areas
        mainTextArea.setScrollTop(0);
        lineNumbers.setScrollTop(0);
        mainTextArea.scrollTopProperty().addListener((obs, oldVal, newVal) -> {
            lineNumbers.setScrollTop(newVal.doubleValue());
        });

        // Container for line numbers and main text area
        HBox textAreaContainer = new HBox(0);
        textAreaContainer.getChildren().addAll(lineNumbers, mainTextArea);
        HBox.setHgrow(mainTextArea, Priority.ALWAYS);

        // Add both components to the right panel
        rightPanel.getChildren().addAll(resultPane, textAreaContainer);

        // Set the vertical growth constraints
        VBox.setVgrow(resultPane, Priority.NEVER);
        VBox.setVgrow(textAreaContainer, Priority.ALWAYS);

        // Set the first TitledPane to take 1/5 of the height for better display
        resultPane.prefHeightProperty().bind(rightPanel.heightProperty().multiply(0.20));

        return rightPanel;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
