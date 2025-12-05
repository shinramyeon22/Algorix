package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LexicalAnalysis {

    private final List<String> errors;   
    private boolean isValid;
    private int totalTokenCount;
    private final List<String> tokenizedLines;

    private final String[] dataTypes = {"int", "double", "float", "boolean", "char", "long", "byte", "short", "String"};
    private final String assignmentOperators = "=";
    private final String delimiter = ";";
    private final String identifierRegex = "[A-Za-z_$][A-Za-z0-9_$]*";

    public LexicalAnalysis() {
        errors = new ArrayList<>();
        tokenizedLines = new ArrayList<>();
        isValid = true;
    }
    
    public String analyze(String sourceCode) {
        errors.clear();
        isValid = true;
        totalTokenCount = 0;
        tokenizedLines.clear();
        
        int lineNum = 1;

        for (String codeLine : sourceCode.split("\n")) {
            String trimmed = codeLine.trim();
            if (trimmed.isEmpty()) {
                lineNum++;
                continue;
            }
            if (!isVariableDeclaration(trimmed, lineNum)) {
                isValid = false;
                errors.add("Line " + lineNum + ": Only variable declarations are allowed. Found: " + trimmed);
            }
            lineNum++;
        }
        
        return generateResult();
    }
    
    private boolean isVariableDeclaration(String line, int lineNum) {   
        if (!line.contains(";") && !line.contains("=")) {
            return false;   
        }

        // Tokenize and categorize lexemes for this line
        String lineTokens = categorizeLexemes(line, lineNum);
        tokenizedLines.add(lineTokens);
        
        String[] tokens = line.split("\\s+"); 
        
        if (tokens.length == 0) {
            return false;
        }
        
        for (String type : dataTypes) {
            if (tokens[0].equals(type)) { 
                return true;
            }
        }
        return false;
    }
    
    private String categorizeLexemes(String line, int lineNum) {
        // Normalize quotes
        line = line.replace('"', '"').replace('"', '"');
        
        // Pattern to extract tokens (strings in quotes or non-whitespace)
        Pattern p = Pattern.compile("\"[^\"]*\"|\\S+");
        Matcher m = p.matcher(line);
        
        List<String> tokens = new ArrayList<>();
        while (m.find()) {
            tokens.add(m.group());
        }
        
        List<String> lexemeCategories = new ArrayList<>();
        boolean expectIdentifier = false;
        int lineTokenCount = 0;
        
        for (String token : tokens) {
            boolean hadDelimiter = token.endsWith(delimiter);
            String t = token;
            
            // Remove trailing delimiters
            if (hadDelimiter) {
                while (t.endsWith(delimiter)) {
                    t = t.substring(0, t.length() - 1);
                }
            }
            
            if (t.isEmpty()) {
                lexemeCategories.add("<delimiter>");
                lineTokenCount++;
                expectIdentifier = false;
                continue;
            }
            
            // Check for data types
            boolean matched = false;
            for (String dt : dataTypes) {
                if (t.equals(dt)) {
                    lexemeCategories.add("<data_type>");
                    lineTokenCount++;
                    expectIdentifier = true;
                    matched = true;
                    break;
                }
            }
            
            if (matched) {
                if (hadDelimiter) {
                    lexemeCategories.add("<delimiter>");
                    lineTokenCount++;
                    expectIdentifier = false;
                }
                continue;
            }
            
            // Check for identifier after data type
            if (expectIdentifier) {
                if (t.matches(identifierRegex)) {
                    lexemeCategories.add("<identifier>");
                    lineTokenCount++;
                    expectIdentifier = false;
                    if (hadDelimiter) {
                        lexemeCategories.add("<delimiter>");
                        lineTokenCount++;
                    }
                    continue;
                } else {
                    expectIdentifier = false;
                }
            }
            
            // Check for assignment operators
    
        if(t.equals(assignmentOperators)){
            lexemeCategories.add("<assignment_operator>");
            lineTokenCount++;
            if (hadDelimiter) {
                lexemeCategories.add("<delimiter>");
                lineTokenCount++;
                expectIdentifier = false;
            }
            continue; // Skip to next token

        }

            if (t.matches("[+-]?\\d+(\\.\\d+)?")) { 
                lexemeCategories.add("<value>");
                lineTokenCount++;
            } else if (t.matches("\".*\"")) { 
                lexemeCategories.add("<value>");
                lineTokenCount++;
            } else if (t.matches(identifierRegex)) {
                lexemeCategories.add("<identifier>");
                lineTokenCount++;
            }
            
            if (hadDelimiter) {
                lexemeCategories.add("<delimiter>");
                lineTokenCount++;
                expectIdentifier = false;
            }
        }
        
        totalTokenCount += lineTokenCount;
        return "Line " + lineNum + " (" + lineTokenCount + " tokens): " + String.join(" ", lexemeCategories);
    }
    
    private String generateResult() {
        StringBuilder result = new StringBuilder();
        
        if (isValid && errors.isEmpty()) {
            result.append("LEXICAL ANALYSIS PASSED\n\n");
            result.append("Total tokens found: ").append(totalTokenCount).append("\n\n");
            
            for (String tokenizedLine : tokenizedLines) {
                result.append(tokenizedLine).append("\n");
            }
        } else {
            result.append("LEXICAL ANALYSIS FAILED\n\n");
            result.append("Errors:\n");
            for (String error : errors) {
                result.append(error).append("\n");
            }
        }
        
        return result.toString();
    }
    
    public boolean isPassed() {
        return isValid && errors.isEmpty();
    }
    
    public int getTokenCount() {
        return totalTokenCount;
    }
    
    public List<String> getTokenizedLines() {
        return new ArrayList<>(tokenizedLines);
    }
}
