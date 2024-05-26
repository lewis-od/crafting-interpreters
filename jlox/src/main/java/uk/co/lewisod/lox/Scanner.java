package uk.co.lewisod.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.co.lewisod.lox.TokenType.*;

public class Scanner {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("and", AND);
        KEYWORDS.put("class", CLASS);
        KEYWORDS.put("else", ELSE);
        KEYWORDS.put("false", FALSE);
        KEYWORDS.put("for", FOR);
        KEYWORDS.put("fun", FUN);
        KEYWORDS.put("if", IF);
        KEYWORDS.put("nil", NIL);
        KEYWORDS.put("or", OR);
        KEYWORDS.put("print", PRINT);
        KEYWORDS.put("return", RETURN);
        KEYWORDS.put("super", SUPER);
        KEYWORDS.put("this", THIS);
        KEYWORDS.put("true", TRUE);
        KEYWORDS.put("var", VAR);
        KEYWORDS.put("while", WHILE);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        var character = advance();
        switch (character) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '"':
                string();
                break;
            default:
                if (isDigit(character)) {
                    number();
                } else if (isAlpha(character)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType tokenType) {
        addToken(tokenType, null);
    }

    private void addToken(TokenType type, Object literal) {
        var text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string literal.");
            return;
        }

        advance(); // Closing quote

        // Trim surrounding quotes
        var value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            // First advance consumes decimal point
            do advance();
            while (isDigit(peek()));
        }

        var value = source.substring(start, current);
        addToken(NUMBER, Double.parseDouble(value));
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        var text = source.substring(start, current);
        var tokenType = KEYWORDS.getOrDefault(text, IDENTIFIER);
        addToken(tokenType);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // Could have made peek() take an argument for num chars to lookahead.
    // Having 2 separate functions makes it clearer restricting to a 2 char
    // lookahead was a conscious decision.
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }
}
