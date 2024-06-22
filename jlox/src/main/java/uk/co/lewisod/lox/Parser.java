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

    // declaration -> classDeclaration | funDeclaration | varDeclaration | statement ;
    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    // classDeclaration -> "class" IDENTIFIER "{" function* "}" ;
    private Stmt classDeclaration() {
        var name = consume(IDENTIFIER, "Expect class name.");

        consume(LEFT_BRACE, "Expect '{' before class body.");
        var methods = new ArrayList<Stmt.Function>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }
        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods);
    }

    // funDeclaration -> "fun" function ;
    // function -> IDENTIFIER "(" parameters? ")" block ;
    private Stmt.Function function(String kind) {
        var name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");

        var parameters = new ArrayList<Token>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    // Not throwing exception as syntax is still valid - error is due to language limitations
                    error(peek(), "Can't have more then 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        // Consuming the left brace here as opposed to in block() let's us report a more useful error message,
        // since we know we're in the middle of a function declaration
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        var body = block();
        return new Stmt.Function(name, parameters, body);
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

    // statement -> exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt | block ;
    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStmt();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    // printStmt -> "print" expression ";" ;
    private Stmt printStatement() {
        var value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    // returnStmt -> "return" expression? ";" ;
    private Stmt returnStmt() {
        var keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    // forStmt -> "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
    public Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after clauses.");

        Stmt body = statement();

        // Desugar the for loop into a while loop
        if (increment != null) {
            // Place the increment statement at the end of the loop body
            body = new Stmt.Block(List.of(body, new Stmt.Expression(increment)));
        }
        if (condition == null) condition = new Expr.Literal(true);
        Stmt loop = new Stmt.While(condition, body);
        if (initializer != null) {
            // Run the initializer before the while loop
            loop = new Stmt.Block(List.of(initializer, loop));
        }

        return loop;
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

    // whileStmt -> "while" "(" expression ")" statement ;
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        var condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        var body = statement();

        return new Stmt.While(condition, body);
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

    // assignment -> IDENTIFIER "=" assignment | logic_or;
    private Expr assignment() {
        var expr = or();

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

    // logic_or -> logic_and ( "or" logic_and )* ;
    private Expr or() {
        var expr = and();

        while(match(OR)) {
            var operator = previous();
            var right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // logic_and -> equality ( "and" equality )* ;
    private Expr and() {
        var expr = equality();

        while (match(AND)) {
            var operator = previous();
            var right = equality();
            expr = new Expr.Logical(expr, operator, right);
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

    // unary -> ( "!" | "-" ) unary | call ;
    private Expr unary() {
        if (match(BANG, MINUS)) {
            var operator = previous();
            var operand = unary();
            return new Expr.Unary(operator, operand);
        }
        return call();
    }

    // call -> primary ( "(" arguments? ")" )* ;
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                var arguments = new ArrayList<Expr>();
                if (!check(RIGHT_PAREN)) {
                    do {
                        if (arguments.size() >= 255) {
                            // Not throwing exception as syntax is still valid - error is due to language limitations
                            error(peek(), "Can't have more than 255 arguments");
                        }
                        arguments.add(expression());
                    } while (match(COMMA));
                }
                var paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
                expr = new Expr.Call(expr, paren, arguments);
            } else {
                break;
            }
        }

        return expr;
    }

    // arguments -> expression ( "," expression )* ;

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
