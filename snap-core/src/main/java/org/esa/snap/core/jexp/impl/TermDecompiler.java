package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.TermVisitor;

/**
 * Created by Norman on 16.06.2015.
 */
public class TermDecompiler implements TermVisitor<String> {

    public String decompile(Term term) {
        return term.accept(this);
    }

    @Override
    public String visit(Term.ConstB term) {
        return term.toString();
    }

    @Override
    public String visit(Term.ConstI term) {
        return term.toString();
    }

    @Override
    public String visit(Term.ConstD term) {
        return term.toString();
    }

    @Override
    public String visit(Term.ConstS term) {
        return term.toString();
    }

    @Override
    public String visit(Term.Ref term) {
        return term.getSymbol().getName();
    }

    @Override
    public String visit(Term.Call term) {
        final StringBuilder sb = new StringBuilder();
        sb.append(term.getFunction().getName());
        Term[] args = term.getArgs();
        sb.append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(decompile(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    @Override
    public String visit(Term.Cond term) {
        Term arg1 = term.getArg(0);
        Term arg2 = term.getArg(1);
        Term arg3 = term.getArg(2);
        String s1 = decompile(arg1);
        String s2 = decompile(arg2);
        String s3 = decompile(arg3);
        if (arg1.pre() >= term.pre()) {
            s1 = "(" + s1 + ")";
        }
        if (arg2.pre() > term.pre()) {
            s2 = "(" + s2 + ")";
        }
        if (arg3.pre() > term.pre()) {
            s3 = "(" + s3 + ")";
        }
        return s1 + " ? " + s2 + " : " + s3;
    }

    @Override
    public String visit(Term.Assign term) {
        return null;
    }

    @Override
    public String visit(Term.NotB term) {
        return unary(term, "!");
    }

    @Override
    public String visit(Term.AndB term) {
        return binary(term, "&&", true);
    }

    @Override
    public String visit(Term.OrB term) {
        return binary(term, "||", true);
    }

    @Override
    public String visit(Term.NotI term) {
        return unary(term, "~");
    }

    @Override
    public String visit(Term.XOrI term) {
        return binary(term, "^", true);
    }

    @Override
    public String visit(Term.AndI term) {
        return binary(term, "&", true);
    }

    @Override
    public String visit(Term.OrI term) {
        return binary(term, "|", true);
    }

    @Override
    public String visit(Term.Neg term) {
        return unary(term, "-");
    }

    @Override
    public String visit(Term.Add term) {
        return binary(term, "+", true);
    }

    @Override
    public String visit(Term.Sub term) {
        return binary(term, "-", false);
    }

    @Override
    public String visit(Term.Mul term) {
        return binary(term, "*", true);
    }

    @Override
    public String visit(Term.Div term) {
        return binary(term, "/", false);
    }

    @Override
    public String visit(Term.Mod term) {
        return binary(term, "%", false);
    }

    @Override
    public String visit(Term.EqB term) {
        return binary(term, "==", true);
    }

    @Override
    public String visit(Term.EqI term) {
        return binary(term, "==", true);
    }

    @Override
    public String visit(Term.EqD term) {
        return binary(term, "==", true);
    }

    @Override
    public String visit(Term.NEqB term) {
        return binary(term, "!=", true);
    }

    @Override
    public String visit(Term.NEqI term) {
        return binary(term, "!=", true);
    }

    @Override
    public String visit(Term.NEqD term) {
        return binary(term, "!=", true);
    }

    @Override
    public String visit(Term.LtI term) {
        return binary(term, "<", true);
    }

    @Override
    public String visit(Term.LtD term) {
        return binary(term, "<", true);
    }

    @Override
    public String visit(Term.LeI term) {
        return binary(term, "<=", true);
    }

    @Override
    public String visit(Term.LeD term) {
        return binary(term, "<=", true);
    }

    @Override
    public String visit(Term.GtI term) {
        return binary(term, ">", true);
    }

    @Override
    public String visit(Term.GtD term) {
        return binary(term, ">", true);
    }

    @Override
    public String visit(Term.GeI term) {
        return binary(term, ">=", true);
    }

    @Override
    public String visit(Term.GeD term) {
        return binary(term, ">=", true);
    }

    private String binary(Term.Binary term, String op, boolean commutative) {
        Term arg1 = term.getArg(0);
        Term arg2 = term.getArg(1);
        String s1 = decompile(arg1);
        String s2 = decompile(arg2);
        if (arg1.pre() > term.pre()) {
            s1 = "(" + s1 + ")";
        }
        if (commutative ? arg2.pre() > term.pre() : arg2.pre() >= term.pre()) {
            s2 = "(" + s2 + ")";
        }
        return s1 + " " + op + " " + s2;
    }

    private String unary(Term.Unary term, String op) {
        Term arg = term.getArg();
        String s = decompile(arg);
        if (arg.pre() > term.pre()) {
            s = "(" + s + ")";
        }
        return op + s;
    }
}
