package com.bc.ceres.binio.expr;

import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;


public class ExpressionBuilder {

    public final static SimpleType BYTE = SimpleType.BYTE;
    public final static SimpleType UBYTE = SimpleType.UBYTE;
    public final static SimpleType SHORT = SimpleType.SHORT;
    public final static SimpleType USHORT = SimpleType.USHORT;
    public final static SimpleType INT = SimpleType.INT;
    public final static SimpleType UINT = SimpleType.UINT;
    public final static SimpleType LONG = SimpleType.LONG;
    public final static SimpleType ULONG = SimpleType.ULONG;
    public final static SimpleType FLOAT = SimpleType.FLOAT;
    public final static SimpleType DOUBLE = SimpleType.DOUBLE;

    public static ConstantExpr CONSTANT(Object value) {
        return new ConstantExpr(value);
    }

    public static ConstantExpr INV(Expression value) {
        return new ConstantExpr(value);
    }

    public static SequenceExpr SEQ(Type elementType, Expression elementCount) {
        return SEQ(CONSTANT(elementType), elementCount);
    }

    public static SequenceExpr SEQ(Expression elementType, int elementCount) {
        return SEQ(elementType, CONSTANT(elementCount));
    }

    public static SequenceExpr SEQ(Type elementType, int elementCount) {
        return SEQ(CONSTANT(elementType), CONSTANT(elementCount));
    }

    public static SequenceExpr SEQ(Expression elementType, Expression elementCount) {
        return new SequenceExpr(elementType, elementCount);
    }

    public static CompoundExpr COMP(String name, CompoundExpr.Member... members) {
        return new CompoundExpr(name, members);
    }

    public static CompoundExpr.Member MEMBER(String name, Type type) {
        return new CompoundExpr.Member(name, CONSTANT(type));
    }

    public static CompoundExpr.Member MEMBER(String name, Expression expression) {
        return new CompoundExpr.Member(name, expression);
    }

    public static ChoiceExpr IF(Expression condition, Type type1, Type type2) {
        return IF(condition, CONSTANT(type1), CONSTANT(type2));
    }

    public static ChoiceExpr IF(Expression condition, Expression expression1, Expression expression2) {
        return new ChoiceExpr(condition, expression1, expression2);
    }

    public static SelectionExpr SELECT(Expression condition, SelectionExpr.Case... cases) {
        return new SelectionExpr(condition, cases);
    }

    public static SelectionExpr.Case CASE(Object value, Type type) {
        return new SelectionExpr.Case(value, CONSTANT(type));
    }

    public static SelectionExpr.Case CASE(Object value, Expression expression) {
        return new SelectionExpr.Case(value, expression);
    }

    public static SelectionExpr.Default DEFAULT(Type type) {
        return DEFAULT(CONSTANT(type));
    }

    public static SelectionExpr.Default DEFAULT(Expression expression) {
        return new SelectionExpr.Default(expression);
    }

    public static IntReferenceExpr IREF(String name) {
        return new IntReferenceExpr(name);
    }

    private ExpressionBuilder() {
    }
}
