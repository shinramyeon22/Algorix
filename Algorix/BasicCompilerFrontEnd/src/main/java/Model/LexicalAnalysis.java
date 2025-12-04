package Model;

import java.util.ArrayList;
import java.util.List;

public class LexicalAnalysis {

    private final List<String> errors;   
    private boolean isValid;

    private final String[] dataTypes = {"int", "double", "float", "boolean", "char", "long", "byte", "short", "String"};

    public LexicalAnalysis() {
        errors = new ArrayList<>();
        isValid = true;
    }
    
    public String analyze(String sourceCode) {
        errors.clear();
        isValid = true;
        StringBuilder result = new StringBuilder();
        int lineNum = 1;

        for (String codeLine : sourceCode.split("\n")) {
            String trimmed = codeLine.trim();
            if (trimmed.isEmpty()) {
                lineNum++;
                continue;
            }
            if (!isVariableDeclaration(trimmed)) {
                isValid = false;
                errors.add("Line " + lineNum + ": Only variable declarations are allowed. Found: " + trimmed);
            }
            lineNum++;
        }
        // Generate result
        if (isValid && errors.isEmpty()) {
            result.append("LEXICAL ANALYSIS PASSED\n\n");

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
        if (!line.contains(";") && !line.contains("=")) {
            return false;   
        }

        String[] tokens = line.split("\\s+"); 
        for(String type: dataTypes) {
            if (tokens[0].equals(type)) { 
                return true;
            }
        }
        return false;
    
    }
    public boolean isPassed() {
        return isValid && errors.isEmpty(); // Overall validity
    }
}
