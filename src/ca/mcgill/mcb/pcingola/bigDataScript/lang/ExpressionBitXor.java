package ca.mcgill.mcb.pcingola.bigDataScript.lang;

import org.antlr.v4.runtime.tree.ParseTree;

import ca.mcgill.mcb.pcingola.bigDataScript.run.BigDataScriptThread;

/**
 * A bitwise XOR
 *
 * @author pcingola
 */
public class ExpressionBitXor extends ExpressionBit {

	public ExpressionBitXor(BigDataScriptNode parent, ParseTree tree) {
		super(parent, tree);
	}

	/**
	 * Evaluate an expression
	 */
	@Override
	public void runStep(BigDataScriptThread bdsThread) {
		left.run(bdsThread);
		right.run(bdsThread);
		bdsThread.push(left.popInt(bdsThread) ^ right.popInt(bdsThread));
	}

	@Override
	protected String op() {
		return "^";
	}

}
