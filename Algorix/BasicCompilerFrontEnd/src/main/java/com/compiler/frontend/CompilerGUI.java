package com.compiler.frontend;

import Controller.FileChooser;
import Model.LexicalAnalysis;
import Model.SemanticAnalysis;
import Model.SyntaxAnalysis;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class CompilerGUI extends Application {
    private TextArea mainTextArea;
    private TextArea resultTextArea;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        Label title = new Label("Algorix Mini Java Compiler");
        title.getStyleClass().add("app-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        root.setTop(title);

        // Left panel (buttons)
        VBox leftPanel = createLeftPanel();
        root.setLeft(leftPanel);

        VBox rightPanel = createRightPanel();
        root.setCenter(rightPanel);

        Scene scene = new Scene(root, 1100, 720);
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

        Button openFileBtn = new Button("Open File");
        openFileBtn.getStyleClass().add("primary-action");
        Button lexicalBtn = new Button("Lexical Analysis");
        Button syntaxBtn = new Button("Syntax Analysis");
        Button semanticBtn = new Button("Semantic Analysis");
        Button clearBtn = new Button("Clear");

        lexicalBtn.setDisable(true);
        syntaxBtn.setDisable(true);
        semanticBtn.setDisable(true);

        clearBtn.setOnAction(event -> {
            mainTextArea.clear();
            resultTextArea.clear();
            lexicalBtn.setDisable(true);
            syntaxBtn.setDisable(true);
            semanticBtn.setDisable(true);
            openFileBtn.setDisable(false);
        });

        openFileBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            String fileContent = fileChooser.openFile();

            if (fileContent != null && !fileContent.isEmpty()) {
                mainTextArea.setText(fileContent);
                openFileBtn.setDisable(true);
                lexicalBtn.setDisable(false);
                syntaxBtn.setDisable(true);
                semanticBtn.setDisable(true);
                resultTextArea.clear();
            }
        });

        lexicalBtn.setOnAction(event -> {
            String sourceCode = mainTextArea.getText();
            if (sourceCode == null || sourceCode.trim().isEmpty()) {
                resultTextArea.setText("Error: Please load a file or enter source code first.");
                syntaxBtn.setDisable(true);
            } else {
                LexicalAnalysis lexical = new LexicalAnalysis();
                String result = lexical.analyze(sourceCode);
                resultTextArea.setText(result);

                if (lexical.isPassed()) {
                    syntaxBtn.setDisable(false);
                    lexicalBtn.setDisable(true);
                } else {
                    syntaxBtn.setDisable(true);
                }
            }
        });

        syntaxBtn.setOnAction(event -> {
            String sourceCode = mainTextArea.getText();
            if (sourceCode == null || sourceCode.trim().isEmpty()) {
                resultTextArea.setText("Error: Please load a file or enter source code first.");
                semanticBtn.setDisable(true);
            } else {
                SyntaxAnalysis syntax = new SyntaxAnalysis();
                String result = syntax.analyze(sourceCode);
                resultTextArea.setText(result);

                if (syntax.isPassed()) {
                    semanticBtn.setDisable(false);
                    syntaxBtn.setDisable(true);
                } else {
                    semanticBtn.setDisable(true);
                }
            }
        });

        semanticBtn.setOnAction(event -> {
            String sourceCode = mainTextArea.getText();
            if (sourceCode == null || sourceCode.trim().isEmpty()) {
                resultTextArea.setText("Error: Please load a file or enter source code first.");
            } else {
                SemanticAnalysis semantic = new SemanticAnalysis();
                String result = semantic.analyze(sourceCode);
                resultTextArea.setText(result);
                if (semantic.isPassed()) {
                    semanticBtn.setDisable(true);
                }
            }
        });

        openFileBtn.setMaxWidth(Double.MAX_VALUE);
        lexicalBtn.setMaxWidth(Double.MAX_VALUE);
        syntaxBtn.setMaxWidth(Double.MAX_VALUE);
        semanticBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);

        leftPanel.getChildren().addAll(openFileBtn, lexicalBtn, syntaxBtn, semanticBtn, clearBtn);

        return leftPanel;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));

        TitledPane resultPane = new TitledPane();
        resultPane.setText("Compiler Output");
        resultPane.setCollapsible(false);
        resultPane.getStyleClass().add("result-pane");

        resultTextArea = new TextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setWrapText(true);
        resultTextArea.getStyleClass().add("compiler-result-area");
        resultPane.setContent(resultTextArea);

        mainTextArea = new TextArea();
        mainTextArea.setWrapText(true);

        TextArea lineNumbers = new TextArea();
        lineNumbers.setEditable(false);
        lineNumbers.setWrapText(false);
        lineNumbers.setPrefWidth(55);
        lineNumbers.getStyleClass().add("line-numbers");

        // Sync line numbers
        mainTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int lines = newVal.split("\n", -1).length;
            StringBuilder lineNumbersText = new StringBuilder();
            for (int i = 1; i <= lines; i++) {
                lineNumbersText.append(i).append("\n");
            }
            lineNumbers.setText(lineNumbersText.toString());
        });

        // Sync scrolling
        mainTextArea.scrollTopProperty().addListener((obs, oldVal, newVal) ->
                lineNumbers.setScrollTop(newVal.doubleValue())
        );

        HBox textAreaContainer = new HBox(0);
        textAreaContainer.getChildren().addAll(lineNumbers, mainTextArea);
        HBox.setHgrow(mainTextArea, Priority.ALWAYS);


        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(resultPane, textAreaContainer);
        splitPane.setDividerPositions(0.25); // Initial size: 25% output, 75% code (user can drag!)

        // Make the layout grow properly
        SplitPane.setResizableWithParent(resultPane, Boolean.FALSE);

        // Add SplitPane to right panel and let it fill all space
        rightPanel.getChildren().add(splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        return rightPanel;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
