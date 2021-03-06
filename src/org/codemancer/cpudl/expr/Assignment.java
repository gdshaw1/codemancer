// This file is part of Codemancer.
// Copyright 2014 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.codemancer.cpudl.expr;

import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import org.codemancer.cpudl.Context;
import org.codemancer.cpudl.State;
import org.codemancer.cpudl.CpudlParseException;
import org.codemancer.cpudl.type.Type;

/** An expression class to represent an assignment operation. */
public class Assignment extends BinaryExpression {
	/** Construct assignment operation from two operands.
	 * @param lhs the left-hand argument
	 * @param rhs the right-hand argument
	 */
	public Assignment(Expression lhs, Expression rhs) {
		super(rhs.getType(), lhs, rhs);
	}

	public Expression partialClone(Expression lhs, Expression rhs) {
		return new Assignment(lhs, rhs);
	}

	public String getSymbol() {
		return "=";
	}

	public Expression evaluate(State state) {
		Expression evalRhs = getRhs().evaluate(state).simplify();
		getLhs().assign(state, evalRhs);
		return evalRhs;
	}

	public void listAssignments(List<Assignment> uncond, List<Assignment> cond, boolean isCond) {
		if (!isCond) {
			uncond.add(this);
		} else {
			cond.add(this);
		}
		getLhs().listAssignments(uncond, cond, isCond);
		getRhs().listAssignments(uncond, cond, isCond);
	}

	/** Make assignment operation from XML element.
	 * @param ctx the context of this expression
	 * @param the expression as an XML element
	 * @return the expression as an object
	 */
	public static Expression make(Context ctx, Element element) throws CpudlParseException {
		ArrayList<Expression> operands = new ArrayList<Expression>();
		Node child = element.getFirstChild();
		while (child != null) {
			Expression operand = ctx.makeExpression(child);
			if (operand != null) {
				operands.add(operand);
			}
			child = child.getNextSibling();
		}
		if (operands.size() != 2) {
			throw new CpudlParseException(element, "two operands expected for assignment");
		}
		return new Assignment(operands.get(0), operands.get(1));
	}
}
