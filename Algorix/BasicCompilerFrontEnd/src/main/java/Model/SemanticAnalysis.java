package Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticAnalysis {

    private List<String> errors;
    private Map<String, String> declaredVariables; // varName -> type

    // Patterns
    private static final Pattern DECL_LINE_PATTERN = Pattern.compile(
        "^\\s*(?:(?:public|private|protected|static|final|transient|volatile)\\s+)*" + // optional modifiers
        "(int|double|float|boolean|char|long|byte|short|String)(?:\\s*\\[\\s*\\])*\\s+(.+);\\s*$"
    );

    private static final Pattern VAR_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern INT_LITERAL = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern FLOAT_LITERAL = Pattern.compile("^[+-]?\\d+\\.\\d+([eE][+-]?\\d+)?$");
    private static final Pattern SCI_NOTATION = Pattern.compile("^[+-]?\\d+(?:\\.\\d+)?[eE][+-]?\\d+$");
    private static final Pattern CHAR_LITERAL = Pattern.compile("^'.'$");
    private static final Pattern STRING_LITERAL = Pattern.compile("^\".*\"$");
    private static final Pattern BOOLEAN_LITERAL = Pattern.compile("^(true|false)$");

    public SemanticAnalysis() {
        errors = new ArrayList<>();
        declaredVariables = new LinkedHashMap<>();
    }

    public String analyze(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return "✗ FAILED: No source code provided";
        }

        errors.clear();
        declaredVariables.clear();

        String code = removeComments(sourceCode);
        List<String> lines = Arrays.asList(code.split("\\r?\\n"));

        int lineNum = 1;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                lineNum++;
                continue;
            }

            // Try match as declaration line
            Matcher declMatcher = DECL_LINE_PATTERN.matcher(line);
            if (declMatcher.matches()) {
                String baseType = declMatcher.group(1);
                String rest = declMatcher.group(2); // var list, possibly with initializers
                analyzeDeclarationLine(baseType, rest, lineNum);
            }
            lineNum++;
        }

        StringBuilder result = new StringBuilder();
        if (errors.isEmpty()) {
            result.append("SEMANTIC ANALYSIS PASSED\n");
        } else {
            result.append("SEMANTIC ANALYSIS FAILED\n\n");
            result.append("Errors:\n");
            for (int i = 0; i < errors.size(); i++) {
                result.append(errors.get(i)).append("\n");
            }
        }
        return result.toString();
    }

    public boolean isPassed() {
        return errors.isEmpty();
    }

    private void analyzeDeclarationLine(String baseType, String varListStr, int lineNum) {
        // Split by commas at top level (ignore commas in string literals)
        List<String> varParts = splitTopLevelCommas(varListStr);

        for (String part : varParts) {
            String p = part.trim();
            if (p.isEmpty()) {
                errors.add("Line " + lineNum + ": Empty declaration part");
                continue;
            }

            String varName;
            String initializer = null;

            int eqIdx = indexOfTopLevelEquals(p);
            if (eqIdx >= 0) {
                varName = p.substring(0, eqIdx).trim();
                initializer = p.substring(eqIdx + 1).trim();
            } else {
                varName = p;
            }

            // Remove array bracket from variable name if present (e.g., nums[] or nums[ ])
            varName = varName.replaceAll("\\[\\s*\\]", "").trim();

            if (!VAR_NAME_PATTERN.matcher(varName).matches()) {
                errors.add("Line " + lineNum + ": Invalid variable name '" + varName + "'");
                continue;
            }

            // Duplicate check
            if (declaredVariables.containsKey(varName)) {
                errors.add("Line " + lineNum + ": Variable '" + varName + "' already declared");
                continue;
            }

            // If initializer present, check compatibility
            if (initializer != null && !initializer.isEmpty()) {
                // Basic trim trailing semicolons if any (shouldn't be, since we matched the line ending)
                if (initializer.endsWith(";")) {
                    initializer = initializer.substring(0, initializer.length() - 1).trim();
                }

                String typeErr = checkTypeCompatibility(baseType, initializer, varName, lineNum);
                if (typeErr != null) {
                    errors.add(typeErr);
                    continue;
                }
            }

            // If passed, add declared variable
            declaredVariables.put(varName, baseType);
        }
    }

    private String checkTypeCompatibility(String declaredType, String expr, String varName, int lineNum) {
        // Quick inference of expression type
        // If expression is in parentheses, strip outer parentheses
        expr = expr.trim();
        while (expr.startsWith("(") && expr.endsWith(")")) {
            // Only strip if parentheses match (simple)
            expr = expr.substring(1, expr.length() - 1).trim();
        }

        // If expression is a single literal/token, handle directly
        if (STRING_LITERAL.matcher(expr).matches()) {
            if (!declaredType.equals("String")) {
                return "Line " + lineNum + ": Type mismatch - cannot assign String literal to " + declaredType + " '" + varName + "'";
            }
            return null;
        }

        if (CHAR_LITERAL.matcher(expr).matches()) {
            if (!declaredType.equals("char")) {
                return "Line " + lineNum + ": Type mismatch - cannot assign char literal to " + declaredType + " '" + varName + "'";
            }
            return null;
        }

        if (BOOLEAN_LITERAL.matcher(expr).matches()) {
            if (!declaredType.equals("boolean")) {
                return "Line " + lineNum + ": Type mismatch - cannot assign boolean literal to " + declaredType + " '" + varName + "'";
            }
            return null;
        }

        if (INT_LITERAL.matcher(expr).matches()) {
            // integer literal can be assigned to any numeric integral types or wider numeric types
            if (isNumericType(declaredType)) {
                return null;
            } else {
                return "Line " + lineNum + ": Type mismatch - cannot assign integer literal to " + declaredType + " '" + varName + "'";
            }
        }

        if (FLOAT_LITERAL.matcher(expr).matches() || SCI_NOTATION.matcher(expr).matches()) {
            if (declaredType.equals("double") || declaredType.equals("float")) {
                return null;
            } else if (isIntegralType(declaredType)) {
                return "Line " + lineNum + ": Type mismatch - cannot assign floating literal to integral type " + declaredType + " '" + varName + "'";
            } else {
                return "Line " + lineNum + ": Type mismatch - cannot assign floating literal to " + declaredType + " '" + varName + "'";
            }
        }

        // Expression contains operators or multiple tokens. Tokenize and check each token
        List<String> tokens = tokenizeExpression(expr);

        // Track if expression contains a string literal or String variable
        boolean hasString = false;
        boolean hasFloat = false;
        boolean hasDouble = false;
        boolean hasIntegral = false;
        boolean hasBoolean = false;

        for (String tok : tokens) {
            if (tok.isEmpty()) continue;

            if (STRING_LITERAL.matcher(tok).matches()) {
                hasString = true;
                continue;
            }
            if (CHAR_LITERAL.matcher(tok).matches()) {
                // char is integral-like for operations but treat specially
                hasIntegral = true;
                continue;
            }
            if (BOOLEAN_LITERAL.matcher(tok).matches()) {
                hasBoolean = true;
                continue;
            }
            if (INT_LITERAL.matcher(tok).matches()) {
                hasIntegral = true;
                continue;
            }
            if (FLOAT_LITERAL.matcher(tok).matches() || SCI_NOTATION.matcher(tok).matches()) {
                hasDouble = true;
                continue;
            }

            // Otherwise token might be a variable reference
            if (VAR_NAME_PATTERN.matcher(tok).matches()) {
                if (!declaredVariables.containsKey(tok)) {
                    return "Line " + lineNum + ": Undefined variable '" + tok + "' used in assignment to '" + varName + "'";
                } else {
                    String refType = declaredVariables.get(tok);
                    if (refType.equals("String")) hasString = true;
                    else if (refType.equals("double") || refType.equals("float")) hasDouble = true;
                    else if (isIntegralType(refType)) hasIntegral = true;
                    else if (refType.equals("boolean")) hasBoolean = true;
                }
            }

            // Other tokens (operators, parentheses) ignore
        }

        // Now determine expression overall type (very conservative)
        String exprType;
        if (hasString) exprType = "String";
        else if (hasDouble) exprType = "double";
        else if (hasFloat) exprType = "float";
        else if (hasIntegral) exprType = "int"; // treat all integral as int for compatibility checks
        else if (hasBoolean) exprType = "boolean";
        else {
            // Could not infer; be conservative: allow if declaredType is not String/boolean or char mismatch
            exprType = "unknown";
        }

        // Compatibility rules
        if (exprType.equals("String")) {
            if (!declaredType.equals("String")) {
                return "Line " + lineNum + ": Type mismatch - expression evaluates to String but variable '" + varName + "' is " + declaredType;
            }
            return null;
        }

        if (exprType.equals("boolean")) {
            if (!declaredType.equals("boolean")) {
                return "Line " + lineNum + ": Type mismatch - expression evaluates to boolean but variable '" + varName + "' is " + declaredType;
            }
            return null;
        }

        if (exprType.equals("double") || exprType.equals("float")) {
            if (declaredType.equals("double") || declaredType.equals("float")) return null;
            return "Line " + lineNum + ": Type mismatch - expression evaluates to floating type but variable '" + varName + "' is " + declaredType;
        }

        if (exprType.equals("int")) {
            if (isNumericType(declaredType)) return null; // integral -> can fit into wider numeric types (we're being permissive)
            return "Line " + lineNum + ": Type mismatch - expression evaluates to integral type but variable '" + varName + "' is " + declaredType;
        }

        // unknown expression type - be permissive but flag if declaredType is String/boolean/char
        if (declaredType.equals("String") || declaredType.equals("boolean") || declaredType.equals("char")) {
            return "Line " + lineNum + ": Unable to verify initializer type for '" + varName + "' declared as " + declaredType;
        }

        return null;
    }

    private boolean isIntegralType(String t) {
        return Arrays.asList("int", "long", "byte", "short", "char").contains(t);
    }

    private boolean isNumericType(String t) {
        return isIntegralType(t) || t.equals("double") || t.equals("float");
    }

    // -----------------------
    // Helpers: split top level commas and equals
    // -----------------------
    private List<String> splitTopLevelCommas(String s) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean escape = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                cur.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                cur.append(c);
                continue;
            }
            if (c == '"' && !inChar) {
                inString = !inString;
                cur.append(c);
                continue;
            }
            if (c == '\'' && !inString) {
                inChar = !inChar;
                cur.append(c);
                continue;
            }
            if (c == ',' && !inString && !inChar) {
                parts.add(cur.toString());
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        if (cur.length() > 0) parts.add(cur.toString());
        return parts;
    }

    // find top-level '=' (not inside quotes or parentheses) - returns index or -1
    private int indexOfTopLevelEquals(String s) {
        boolean inString = false;
        boolean inChar = false;
        boolean escape = false;
        int paren = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"' && !inChar) {
                inString = !inString;
                continue;
            }
            if (c == '\'' && !inString) {
                inChar = !inChar;
                continue;
            }
            if (inString || inChar) continue;
            if (c == '(') paren++;
            else if (c == ')') {
                if (paren > 0) paren--;
            } else if (c == '=' && paren == 0) return i;
        }
        return -1;
    }

    // -----------------------
    // Basic tokenizer for expressions: return identifiers and literals as tokens
    // -----------------------
    private List<String> tokenizeExpression(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean escape = false;

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (escape) {
                cur.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                cur.append(c);
                continue;
            }
            if (c == '"' && !inChar) {
                cur.append(c);
                inString = !inString;
                if (!inString) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
                continue;
            }
            if (c == '\'' && !inString) {
                cur.append(c);
                inChar = !inChar;
                if (!inChar) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
                continue;
            }
            if (inString || inChar) {
                cur.append(c);
                continue;
            }

            // if char is part of identifier/number/underscore/dot/e notation sign
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '+' || c == '-' ) {
                // but treat + and - as part of numeric literal when appropriate; we keep them for literal detection
                cur.append(c);
            } else {
                if (cur.length() > 0) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
                // operators and parentheses are skipped
            }
        }
        if (cur.length() > 0) tokens.add(cur.toString());
        return tokens;
    }

    // -----------------------
    // Robust comment remover: skips comment markers inside string/char literals
    // -----------------------
    private String removeComments(String source) {
        StringBuilder out = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean escape = false;
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);

            if (escape) {
                out.append(c);
                escape = false;
                i++;
                continue;
            }

            if (c == '\\') {
                escape = true;
                out.append(c);
                i++;
                continue;
            }

            if (c == '"' && !inChar) {
                inString = !inString;
                out.append(c);
                i++;
                continue;
            }
            if (c == '\'' && !inString) {
                inChar = !inChar;
                out.append(c);
                i++;
                continue;
            }

            if (!inString && !inChar && c == '/' && i + 1 < source.length()) {
                char n = source.charAt(i + 1);
                if (n == '/') {
                    // single-line comment: skip until \n
                    i += 2;
                    while (i < source.length() && source.charAt(i) != '\n') i++;
                    // keep newline if present
                    if (i < source.length() && source.charAt(i) == '\n') {
                        out.append('\n');
                        i++;
                    }
                    continue;
                } else if (n == '*') {
                    // multi-line comment: skip until */
                    i += 2;
                    while (i + 1 < source.length()) {
                        if (source.charAt(i) == '*' && source.charAt(i + 1) == '/') {
                            i += 2;
                            break;
                        }
                        i++;
                    }
                    continue;
                }
            }

            out.append(c);
            i++;
        }
        return out.toString();
    }
}
