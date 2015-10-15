/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.jexp;


/**
 * Visitor support for the {@link Term} class.
 *
 * @author Norman Fomferra
 */
public interface TermVisitor<T> {
    T visit(Term.ConstB term);

    T visit(Term.ConstI term);

    T visit(Term.ConstD term);

    T visit(Term.ConstS term);

    T visit(Term.Ref term);

    T visit(Term.Call term);

    T visit(Term.Cond term);

    T visit(Term.Assign term);

    T visit(Term.NotB term);

    T visit(Term.AndB term);

    T visit(Term.OrB term);

    T visit(Term.NotI term);

    T visit(Term.XOrI term);

    T visit(Term.AndI term);

    T visit(Term.OrI term);

    T visit(Term.Neg term);

    T visit(Term.Add term);

    T visit(Term.Sub term);

    T visit(Term.Mul term);

    T visit(Term.Div term);

    T visit(Term.Mod term);

    T visit(Term.EqB term);

    T visit(Term.EqI term);

    T visit(Term.EqD term);

    T visit(Term.NEqB term);

    T visit(Term.NEqI term);

    T visit(Term.NEqD term);

    T visit(Term.LtI term);

    T visit(Term.LtD term);

    T visit(Term.LeI term);

    T visit(Term.LeD term);

    T visit(Term.GtI term);

    T visit(Term.GtD term);

    T visit(Term.GeI term);

    T visit(Term.GeD term);
}
