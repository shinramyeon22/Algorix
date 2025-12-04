package Model;

import java.util.ArrayList;
import java.util.List;

public class SyntaxAnalysis {
    
    private List<String> errors;
    
    private String[] dataTypes = {"int", "double", "float", "boolean", "char", "long", "byte", "short", "String", "void"};
    
    public SyntaxAnalysis() {
        errors = new ArrayList<>();
    }
    
    public String analyze(String sourceCode) { 
        errors.clear();
        StringBuilder result = new StringBuilder();
        
        String codeWithoutComments = removeComments(sourceCode);
        
        // Check that ALL non-empty lines are variable declarations
        boolean hasVariableDeclaration = false;
        int lineNum = 1;
        
        for (String codeLine : codeWithoutComments.split("\n")) {
            String trimmed = codeLine.trim();
            if (trimmed.isEmpty()) {
                lineNum++;
                continue;
            }
            
            // Check if line is a variable declaration
            if (isVariableDeclaration(trimmed)) {
                hasVariableDeclaration = true;
                checkVariableDeclarationSyntax(trimmed, lineNum);
            } else {
                // Non-variable declaration code found - FAIL
                errors.add("Line " + lineNum + ": Only variable declarations are allowed. Found: " + trimmed);
            }
            lineNum++;
        }
        
        // Check if we have at least one variable declaration
        if (!hasVariableDeclaration) {
            errors.add("No variable declarations found in the code");
        }
        
        if (errors.isEmpty()) {
            result.append("SYNTAX ANALYSIS PASSED\n\n");
        } else {
            result.append("SYNTAX ANALYSIS FAILED\n\n");
            result.append("Errors:\n");
            for (int i = 0; i < errors.size(); i++) {
                result.append(errors.get(i)).append("\n");
            }
        }
        
        return result.toString();
    }
    
    private boolean isVariableDeclaration(String line) {
        String trimmed = line.trim();
        for (String type : dataTypes) {
            if (trimmed.contains(type) && (trimmed.contains(";") || trimmed.contains("="))) {
                return true;
            }
        }
        return false;
    }
    
    private void checkVariableDeclarationSyntax(String line, int lineNum) {
        String trimmed = line.trim();
        
        // Must end with semicolon
        if (!trimmed.endsWith(";")) {
            errors.add("Line " + lineNum + ": Variable declaration must end with semicolon - " + trimmed);
            return;
        }
        
        String withoutSemicolon = trimmed.substring(0, trimmed.length() - 1).trim();
        
        // Check for invalid operators like == instead of single =
        if (withoutSemicolon.contains("==")) {
            errors.add("Line " + lineNum + ": Invalid operator '==' used instead of '=' - " + trimmed);
            return;
        }
        
        // Split by equals to separate declaration from assignment
        String[] parts = withoutSemicolon.split("=", 2);
        String declarationPart = parts[0].trim();
        
        // If there's an assignment, check it's not empty
        if (parts.length == 2 && parts[1].trim().isEmpty()) {
            errors.add("Line " + lineNum + ": Assignment value cannot be empty - " + trimmed);
            return;
        }
        
        // Extract type and variable name from declaration part
        // Pattern: [modifiers] type variableName
        String[] tokens = declarationPart.split("\\s+");
        
        if (tokens.length < 2) {
            errors.add("Line " + lineNum + ": Missing type or variable name - " + trimmed);
            return;
        }
        
        // For variable declaration, should only have: type variableName (no extra tokens)
        if (tokens.length > 2) {
            errors.add("Line " + lineNum + ": Too many tokens in declaration part '" + declarationPart + "' - " + trimmed);
            return;
        }
        
        // The last token should be the variable name
        String varName = tokens[tokens.length - 1];
        
        // Variable name must be a valid Java identifier
        if (!varName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            errors.add("Line " + lineNum + ": Invalid variable name '" + varName + "' - " + trimmed);
            return;
        }
        
     
        
        // Check if there's a valid type before the variable name
        String type = tokens[tokens.length - 2];
        boolean isValidType = false;
        for (String dt : dataTypes) {
            if (type.equals(dt) || type.startsWith(dt + "<")) {
                isValidType = true;
                break;
            }
        }
        
        // Also allow custom class types
        if (!isValidType && type.matches("^[a-zA-Z_][a-zA-Z0-9_]*(<[^>]*>)?$")) {
            isValidType = true;
        }
        
        if (!isValidType) {
            errors.add("Line " + lineNum + ": Invalid or missing type '" + type + "' - " + trimmed);
        }
    }
    
    public boolean isPassed() {
        return errors.isEmpty();
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
}
