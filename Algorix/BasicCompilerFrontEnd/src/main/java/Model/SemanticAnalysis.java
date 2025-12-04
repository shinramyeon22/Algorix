package Model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticAnalysis {

    private final List<String> errors;
    private final Map<String, DataType> declaredVariables;

    private enum DataType {
        INT, DOUBLE, FLOAT, BOOLEAN, CHAR, LONG, BYTE, SHORT, STRING;

        private static final Map<String, DataType> MAP = new LinkedHashMap<>();
        private static final Set<DataType> INTEGRAL = EnumSet.of(INT, LONG, BYTE, SHORT, CHAR);
        private static final Set<DataType> NUMERIC = EnumSet.of(INT, LONG, BYTE, SHORT, CHAR, DOUBLE, FLOAT);

        static {
            for (DataType t : values()) MAP.put(t.name().toLowerCase(), t);
            MAP.put("String", STRING);
        }

        static DataType from(String s) { return MAP.get(s); }
        boolean isIntegral() { return INTEGRAL.contains(this); }
        boolean isNumeric() { return NUMERIC.contains(this); }
        boolean isFloating() { return this == DOUBLE || this == FLOAT; }
    }

    private static final Pattern DECL_LINE = Pattern.compile(
        "^\\s*(?:(?:public|private|protected|static|final|transient|volatile)\\s+)*" +
        "(int|double|float|boolean|char|long|byte|short|String)(?:\\s*\\[\\s*\\])*\\s+(.+);\\s*$");
    private static final Pattern VAR_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern INT_LIT = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern FLOAT_LIT = Pattern.compile("^[+-]?\\d+\\.\\d+(?:[eE][+-]?\\d+)?$");
    private static final Pattern SCI_NOT = Pattern.compile("^[+-]?\\d+(?:\\.\\d+)?[eE][+-]?\\d+$");
    private static final Pattern CHAR_LIT = Pattern.compile("^'.'$");
    private static final Pattern STR_LIT = Pattern.compile("^\".*\"$");
    private static final Pattern BOOL_LIT = Pattern.compile("^(?:true|false)$");

    public SemanticAnalysis() {
        errors = new ArrayList<>();
        declaredVariables = new LinkedHashMap<>();
    }

    public String analyze(String sourceCode) {
        errors.clear();
        declaredVariables.clear();
        String[] lines = sourceCode.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            Matcher m = DECL_LINE.matcher(line);
            if (m.matches()) {
                analyzeDeclaration(m.group(1), m.group(2), i + 1);
            }
        }

        StringBuilder result = new StringBuilder();
        if (errors.isEmpty()) {
            result.append("SEMANTIC ANALYSIS PASSED\n");
        } else {
            result.append("SEMANTIC ANALYSIS FAILED\n\nErrors:\n");
            for (String err : errors) result.append(err).append('\n');
        }
        return result.toString();
    }

    public boolean isPassed() { return errors.isEmpty(); }

    private void analyzeDeclaration(String typeStr, String varList, int line) {
        DataType type = DataType.from(typeStr);
        for (String part : splitCommas(varList)) {
            part = part.trim();

            int eq = findEquals(part);
            String name = (eq >= 0 ? part.substring(0, eq) : part).replaceAll("\\[\\s*\\]", "").trim();
            String init = eq >= 0 ? part.substring(eq + 1).replace(";", "").trim() : null;

            if (declaredVariables.containsKey(name)) {
                errors.add("Line " + line + ": Variable '" + name + "' already declared");
                continue;
            }
            if (init != null && !init.isEmpty()) {
                String err = checkType(type, init, name, line);
                if (err != null) {
                    errors.add(err);
                    continue;
                }
            }
            declaredVariables.put(name, type);
        }
    }

    private String checkType(DataType declared, String expr, String var, int line) {
        expr = expr.trim();
        while (expr.startsWith("(") && expr.endsWith(")")) 
            expr = expr.substring(1, expr.length() - 1).trim();

        // Single literals
        if (STR_LIT.matcher(expr).matches())
            return declared != DataType.STRING ? err(line, "String literal", declared, var) : null;
        if (CHAR_LIT.matcher(expr).matches())
            return declared != DataType.CHAR ? err(line, "char literal", declared, var) : null;
        if (BOOL_LIT.matcher(expr).matches())
            return declared != DataType.BOOLEAN ? err(line, "boolean literal", declared, var) : null;
        if (INT_LIT.matcher(expr).matches())
            return !declared.isNumeric() ? err(line, "integer literal", declared, var) : null;
        if (FLOAT_LIT.matcher(expr).matches() || SCI_NOT.matcher(expr).matches()) {
            if (declared.isFloating()) return null;
            return err(line, declared.isIntegral() ? "floating literal to integral type" : "floating literal", declared, var);
        }

        // Complex expressions
        boolean hasStr = false, hasFloat = false, hasInt = false, hasBool = false;
        for (String tok : tokenize(expr)) {
            if (tok.isEmpty()) continue;
            if (STR_LIT.matcher(tok).matches()) { hasStr = true; continue; }
            if (CHAR_LIT.matcher(tok).matches() || INT_LIT.matcher(tok).matches()) { hasInt = true; continue; }
            if (BOOL_LIT.matcher(tok).matches()) { hasBool = true; continue; }
            if (FLOAT_LIT.matcher(tok).matches() || SCI_NOT.matcher(tok).matches()) { hasFloat = true; continue; }

            if (VAR_NAME.matcher(tok).matches()) {
                DataType ref = declaredVariables.get(tok);
                if (ref == null) return "Line " + line + ": Undefined variable '" + tok + "' in '" + var + "'";
                if (ref == DataType.STRING) hasStr = true;
                else if (ref.isFloating()) hasFloat = true;
                else if (ref.isIntegral()) hasInt = true;
                else if (ref == DataType.BOOLEAN) hasBool = true;
            }
        }

        DataType exprType = hasStr ? DataType.STRING : hasFloat ? DataType.DOUBLE : hasInt ? DataType.INT : hasBool ? DataType.BOOLEAN : null;
        
        if (exprType == DataType.STRING)
            return declared != DataType.STRING ? exprErr(line, "String", declared, var) : null;
        if (exprType == DataType.BOOLEAN)
            return declared != DataType.BOOLEAN ? exprErr(line, "boolean", declared, var) : null;
        if (exprType == DataType.DOUBLE)
            return !declared.isFloating() ? exprErr(line, "floating type", declared, var) : null;
        if (exprType == DataType.INT)
            return !declared.isNumeric() ? exprErr(line, "integral type", declared, var) : null;

        if (declared == DataType.STRING || declared == DataType.BOOLEAN || declared == DataType.CHAR)
            return "Line " + line + ": Unable to verify initializer for '" + var + "' as " + declared;
        return null;
    }

    private String err(int line, String lit, DataType type, String var) {
        return "Line " + line + ": Type mismatch - cannot assign " + lit + " to " + type + " '" + var + "'";
    }

    private String exprErr(int line, String exprType, DataType type, String var) {
        return "Line " + line + ": Type mismatch - expression is " + exprType + " but '" + var + "' is " + type;
    }

    private List<String> splitCommas(String s) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false, inChr = false, esc = false;
        
        for (char c : s.toCharArray()) {
            if (esc) { cur.append(c); esc = false; continue; }
            if (c == '\\') { esc = true; cur.append(c); continue; }
            if (c == '"' && !inChr) { inStr = !inStr; cur.append(c); continue; }
            if (c == '\'' && !inStr) { inChr = !inChr; cur.append(c); continue; }
            if (c == ',' && !inStr && !inChr) { parts.add(cur.toString()); cur.setLength(0); continue; }
            cur.append(c);
        }
        if (cur.length() > 0) parts.add(cur.toString());
        return parts;
    }

    private int findEquals(String s) {
        boolean inStr = false, inChr = false, esc = false;
        int paren = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) { esc = false; continue; }
            if (c == '\\') { esc = true; continue; }
            if (c == '"' && !inChr) { inStr = !inStr; continue; }
            if (c == '\'' && !inStr) { inChr = !inChr; continue; }
            if (!inStr && !inChr) {
                if (c == '(') paren++;
                else if (c == ')' && paren > 0) paren--;
                else if (c == '=' && paren == 0) return i;
            }
        }
        return -1;
    }

    private List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false, inChr = false, esc = false;

        for (char c : expr.toCharArray()) {
            if (esc) { cur.append(c); esc = false; continue; }
            if (c == '\\') { esc = true; cur.append(c); continue; }
            if (c == '"' && !inChr) {
                cur.append(c);
                inStr = !inStr;
                if (!inStr) { tokens.add(cur.toString()); cur.setLength(0); }
                continue;
            }
            if (c == '\'' && !inStr) {
                cur.append(c);
                inChr = !inChr;
                if (!inChr) { tokens.add(cur.toString()); cur.setLength(0); }
                continue;
            }
            if (inStr || inChr) { cur.append(c); continue; }

            if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '+' || c == '-') {
                cur.append(c);
            } else if (cur.length() > 0) {
                tokens.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) tokens.add(cur.toString());
        return tokens;
    }
}
