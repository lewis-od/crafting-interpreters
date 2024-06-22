package uk.co.lewisod.lox;

import java.util.ArrayList;

public class AstPrinter implements Expr.Visitor<String> {

    public String print(Expr expression) {
        return expression.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("assign " + expr.name + " = ", expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        var exprs = new ArrayList<Expr>();
        exprs.add(expr.callee);
        exprs.addAll(expr.arguments);
        return parenthesize("call", exprs.toArray(new Expr[0]));
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return parenthesize("get " + expr.name.lexeme, expr.object);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return parenthesize("var " + expr.name + " = ", expr);
    }

    private String parenthesize(String name, Expr... exprs) {
        var builder = new StringBuilder();
        builder.append("(").append(name);
        for (var expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }
}
