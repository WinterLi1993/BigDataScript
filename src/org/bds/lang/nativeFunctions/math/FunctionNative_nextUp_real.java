package org.bds.lang.nativeFunctions.math;

import org.bds.lang.Parameters;
import org.bds.lang.Type;
import org.bds.lang.nativeFunctions.FunctionNative;
import org.bds.run.BdsThread;

public class FunctionNative_nextUp_real extends FunctionNative {
	public FunctionNative_nextUp_real() {
		super();
	}

	@Override
	protected void initFunction() {
		functionName = "nextUp";
		returnType = Type.REAL;

		String argNames[] = { "d" };
		Type argTypes[] = { Type.REAL };
		parameters = Parameters.get(argTypes, argNames);
		addNativeFunctionToScope();
	}

	@Override
	protected Object runFunctionNative(BdsThread bdsThread) {
		return (Double) Math.nextUp(bdsThread.getReal("d"));
	}
}
