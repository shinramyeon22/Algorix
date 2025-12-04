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
        
        int lineNum = 1;
        
        for (String codeLine : sourceCode.split("\n")) {
            String trimmed = codeLine.trim();
            if (trimmed.isEmpty()) {
                lineNum++;
                continue;
            }
            checkVariableDeclarationSyntax(trimmed, lineNum);
            lineNum++;
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
    
    private void checkVariableDeclarationSyntax(String line, int lineNum) {
        String trimmed = line.trim();
        
        if (!trimmed.endsWith(";")) {
            errors.add("Line " + lineNum + ": Variable declaration must end with semicolon");
            return;
        }
        
        String withoutSemicolon = trimmed.substring(0, trimmed.length() - 1).trim();
        
        if (withoutSemicolon.contains("==")) {
            errors.add("Line " + lineNum + ": Invalid operator '==' used instead of '='");
            return;
        }
        String[] parts = withoutSemicolon.split("=", 2);
        String declarationPart = parts[0].trim();
        
        // If there's an assignment, check it's not empty
        if (parts.length == 2 && parts[1].trim().isEmpty()) {
            errors.add("Line " + lineNum + ": Assignment value cannot be empty");
            return;
        }
    
        // Extract type and variable name from declaration part
        String[] tokens = declarationPart.split("\\s+");
        
        if (tokens.length < 2) {
            errors.add("Line " + lineNum + ": Missing type or variable name");
            return;
        }
        
        // For variable declaration, should only have: type variableName (no extra tokens)
        if (tokens.length > 2) {
            errors.add("Line " + lineNum + ": Too many tokens in declaration part '" + declarationPart + "'");
            return;
        }
        
        // The last token should be the variable name
        String varName = tokens[tokens.length - 1];
        
        // Variable name must be a valid Java identifier
        if (!varName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            errors.add("Line " + lineNum + ": Invalid variable name '" + varName + "'");
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
            errors.add("Line " + lineNum + ": Invalid or missing type '" + type + "'");
        }
    }
    
    public boolean isPassed() {
        return errors.isEmpty();
    }
}
