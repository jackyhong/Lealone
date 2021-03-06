/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.lealone.expression;

import org.lealone.dbobject.table.Column;
import org.lealone.dbobject.table.ColumnResolver;
import org.lealone.dbobject.table.TableFilter;
import org.lealone.engine.Session;
import org.lealone.util.StatementBuilder;
import org.lealone.value.Value;
import org.lealone.value.ValueArray;

/**
 * A list of expressions, as in (ID, NAME).
 * The result of this expression is an array.
 */
public class ExpressionList extends Expression {

    private final Expression[] list;

    public ExpressionList(Expression[] list) {
        this.list = list;
    }

    public Value getValue(Session session) {
        Value[] v = new Value[list.length];
        for (int i = 0; i < list.length; i++) {
            v[i] = list[i].getValue(session);
        }
        return ValueArray.get(v);
    }

    public int getType() {
        return Value.ARRAY;
    }

    public void mapColumns(ColumnResolver resolver, int level) {
        for (Expression e : list) {
            e.mapColumns(resolver, level);
        }
    }

    public Expression optimize(Session session) {
        boolean allConst = true;
        for (int i = 0; i < list.length; i++) {
            Expression e = list[i].optimize(session);
            if (!e.isConstant()) {
                allConst = false;
            }
            list[i] = e;
        }
        if (allConst) {
            return ValueExpression.get(getValue(session));
        }
        return this;
    }

    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        for (Expression e : list) {
            e.setEvaluatable(tableFilter, b);
        }
    }

    public int getScale() {
        return 0;
    }

    public long getPrecision() {
        return Integer.MAX_VALUE;
    }

    public int getDisplaySize() {
        return Integer.MAX_VALUE;
    }

    public String getSQL(boolean isDistributed) {
        StatementBuilder buff = new StatementBuilder("(");
        for (Expression e : list) {
            buff.appendExceptFirst(", ");
            buff.append(e.getSQL(isDistributed));
        }
        if (list.length == 1) {
            buff.append(',');
        }
        return buff.append(')').toString();
    }

    public void updateAggregate(Session session) {
        for (Expression e : list) {
            e.updateAggregate(session);
        }
    }

    public boolean isEverything(ExpressionVisitor visitor) {
        for (Expression e : list) {
            if (!e.isEverything(visitor)) {
                return false;
            }
        }
        return true;
    }

    public int getCost() {
        int cost = 1;
        for (Expression e : list) {
            cost += e.getCost();
        }
        return cost;
    }

    public Expression[] getExpressionColumns(Session session) {
        ExpressionColumn[] expr = new ExpressionColumn[list.length];
        for (int i = 0; i < list.length; i++) {
            Expression e = list[i];
            Column col = new Column("C" + (i + 1), e.getType(), e.getPrecision(), e.getScale(), e.getDisplaySize());
            expr[i] = new ExpressionColumn(session.getDatabase(), col);
        }
        return expr;
    }

}
