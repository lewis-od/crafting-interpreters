package uk.co.lewisod.lox;

import java.util.ArrayList;
import java.util.List;

import static uk.co.lewisod.lox.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // program   -> declaration* EOF ;
    public List<Stmt> parse() {
        var statements = new ArrayList<Stmt>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // declaration -> varDeclaration | statement ;
    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    // varDeclaration -> "var" IDENTIFIER ( "=" expression )? ";" ;
    private Stmt varDeclaration() {
        var name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    // statement -> exprStmt | ifStmt | printStmt | block ;
    private Stmt statement() {
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    // printStmt -> "print" expression ";" ;
    private Stmt printStatement() {
        var value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    // ifStmt -> "if" "(" expression ")" statement ( "else" statement )? ;
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        var condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        var thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    // block -> "{" declaration* "}" ;
    private List<Stmt> block() {
        var statements = new ArrayList<Stmt>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    // exprStmt  -> expression ";" ;
    private Stmt expressionStatement() {
        var value = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(value);
    }

    // expression -> assignment ;
    private Expr expression() {
        return assignment();
    }

    // assignment -> IDENTIFIER "=" assignment | equality;
    private Expr assignment() {
        var expr = equality();

        if (match(EQUAL)) {
            var equals = previous();
            var value = assignment();

            if (expr instanceof  Expr.Variable variable) {
                var name = variable.name;
                return new Expr.Assign(name, value);
            }

            // Report but don't throw - no need to panic and synchronize
            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    // equality -> comparison ( ( "!=" | "==" ) comparison )* ;
    private Expr equality() {
        var expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            var operator = previous();
            var right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private Expr comparison() {
        var expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            var operator = previous();
            var right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // term -> factor ( ( "-" | "+" ) factor )* ;
    private Expr term() {
        var expr = factor();
        while (match(MINUS, PLUS)) {
            var operator = previous();
            var right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // factor -> unary ( ( "/" | "*" ) unary )* ;
    private Expr factor() {
        var expr = unary();
        while (match(SLASH, STAR)) {
            var operator = previous();
            var right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // unary -> ( "!" | "-" ) unary | primary ;
    private Expr unary() {
        if (match(BANG, MINUS)) {
            var operator = previous();
            var operand = unary();
            return new Expr.Unary(operator, operand);
        }
        return primary();
    }

    // primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER ;
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            var expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

    private boolean match(TokenType... types) {
        for (var type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
