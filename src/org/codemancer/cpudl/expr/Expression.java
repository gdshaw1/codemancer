// This file is part of Codemancer.
// Copyright 2014 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.codemancer.cpudl.expr;

import java.util.List;
import java.util.Map;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import org.codemancer.cpudl.State;
import org.codemancer.cpudl.Style;
import org.codemancer.cpudl.CpudlParseException;
import org.codemancer.cpudl.CpudlReferenceException;
import org.codemancer.cpudl.type.Type;

/** A class to represent a generic CPUDL expression.
 * CPUDL expressions are symbolic in nature, and may therefore refer
 * to quantities that will not become known until run time.
 */
public abstract class Expression {
	/** The type of this expression. */
	private Type type;

	/** Construct an expression of given type.
	 * @param type the required type
	 */
	public Expression(Type type) {
		this.type = type;
	}

	/** Get the type of this expression.
	 * @return the type
	 */
	public final Type getType() {
		return type;
	}

	/** Convert this expression to a string.
	 * @param st the required style
	 * @return this expression as a string
	 */
	public abstract String unparse(Style style);

	/** Recursively resolve references within this expression.
	 * @param frag the instruction fragment that is currently in scope, or null if none
	 * @param args the arguments that are currently in scope, or null if none
	 * @return the resolved expression
	 */
	public Expression resolveReferences(Fragment frag, Map<String, Expression> args) {
		return this;
	}

	/** Recursively resolve register values within this expression.
	 * @param state the known register values
	 * @return the resolved expression
	 */
	public Expression resolveRegisters(Map<String, Expression> registers) {
		return this;
	}

	/** Recursively evaluate this expression.
	 * @param state the machine state on which to act
	 * @return the evaluated expression
	 */
	public Expression evaluate(State state) {
		return this;
	}

	/** Assign a value to this expression.
	 * This will fail unless the expression is an l-value.
	 * @param state the machine state on which to act
	 * @param value the value to be assigned to this expression
	 */
	public void assign(State state, Expression value) {
		throw new IllegalArgumentException("not an l-value");
	}

	/** Attempt to simplify this expression.
	 * The only hard constraint on the return value is that it
	 * must be mathematically equivalent to this. The default
	 * behaviour is to return a reference to this, which is
	 * always an acceptable outcome, however it is better to
	 * return a simplified expression where that is achievable.
	 * The question of what qualifies as simplification is a
	 * matter for the implementation.
	 * @return a reference to this, or to a simplified equivalent
	 */
	public Expression simplify() {
		return this;
	}

	/** Attempt to solve for a given variable.
	 * A limitation of the current implementation is that it does
	 * not correctly handle the case where the value to be solved
	 * for occurs more than once in the supplied expression.
	 * @param solveFor the variable for which a solution is required,
	 *  in the form of a reference.
	 * @param placeholder an expression (most likely a reference)
	 *  to be used to represent the value of this expression
	 * @return the solution, or null if not found
	 */
	public Expression solve(Reference solveFor, Expression placeholder) {
		return null;
	}

	/** List assignments within this expression.
	 * A conditional expression is any expression which might not be evaluated
	 * (including loops, if the number of iterations could be zero).
	 * @param uncond a list of unconditional assignments to add to
	 * @param cond a list of conditional assignments to add to
	 * @param isCond true if this expression is part of a conditional expression,
	 *  otherwise false
	 */
	public void listAssignments(List<Assignment> uncond,
		List<Assignment> cond, boolean isCond) {}

	/** Add the terms of this expression to an accumulator.
	 * By default this expression is added whole, but if it can be broken
	 * into multiple terms then they should be added separately.
	 * @param acc an accumulator to which the terms should be added
	 * @param multiplier a multiplier to be applied to this expression
	 */
	public void accumulate(Accumulator acc, long multiplier) {
		acc.accumulate(this, multiplier);
	}
}
