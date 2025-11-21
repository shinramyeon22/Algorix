package Model;

import java.util.ArrayList;
import java.util.List;

public class LexicalAnalysis {

    private List<String> errors;
    private boolean isValid;

    private String[] dataTypes = {"int", "double", "float", "boolean", "char", "long", "byte", "short", "String"};

    public LexicalAnalysis() {
        errors = new ArrayList<>();
        isValid = true;
    }

    public String analyze(String sourceCode) {
        errors.clear();
        isValid = true;
        StringBuilder result = new StringBuilder();

        String codeWithoutComments = removeComments(sourceCode);
        
        // Lexical Analysis: Check if line contains a data type and semicolon/assignment (loose check)
        // This allows syntactically invalid lines to pass lexical phase
        boolean hasVariableDeclaration = false;
        int lineNum = 1;

        for (String codeLine : codeWithoutComments.split("\n")) {
            String trimmed = codeLine.trim();
            if (trimmed.isEmpty()) {
                lineNum++;
                continue;
            }

            // Check if line looks like a variable declaration (contains type + semicolon/assignment)
            if (isVariableDeclaration(trimmed)) {
                hasVariableDeclaration = true;
            } else {
                // Non-variable declaration code found - FAIL
                isValid = false;
                errors.add("Line " + lineNum + ": Only variable declarations are allowed. Found: " + trimmed);
            }
            lineNum++;
        }

        // Check if we have at least one variable declaration
        if (!hasVariableDeclaration) {
            isValid = false;
            errors.add("No variable declarations found in the code");
        }

        // Generate result
        if (isValid && errors.isEmpty()) {
            result.append("LEXICAL ANALYSIS PASSED\n\n");
            lineNum = 1;
        } else {
            result.append("LEXICAL ANALYSIS FAILED\n\n");
            result.append("Errors:\n");
            for (int i = 0; i < errors.size(); i++) {
                result.append(errors.get(i)).append("\n");
            }
        }

        return result.toString();
    }
    private boolean isVariableDeclaration(String line) {
        for (String type : dataTypes) {
            if (line.contains(type) && (line.contains(";") || line.contains("="))) {
                return true;
            }
        }
        return false;
    }

    private String removeComments(String sourceCode) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < sourceCode.length()) {
            if (i < sourceCode.length() - 1 && sourceCode.charAt(i) == '/' && sourceCode.charAt(i + 1) == '*') {
                i += 2;
                while (i < sourceCode.length() - 1) {
                    if (sourceCode.charAt(i) == '*' && sourceCode.charAt(i + 1) == '/') {
                        i += 2;
                        break;
                    }
                    i++;
                }
                continue;
            }

            if (i < sourceCode.length() - 1 && sourceCode.charAt(i) == '/' && sourceCode.charAt(i + 1) == '/') {
                while (i < sourceCode.length() && sourceCode.charAt(i) != '\n') {
                    i++;
                }
                if (i < sourceCode.length()) {
                    result.append('\n');
                    i++;
                }
                continue;
            }

            result.append(sourceCode.charAt(i));
            i++;
        }

        return result.toString();
    }

    public boolean isPassed() {
        return isValid && errors.isEmpty();
    }
}

