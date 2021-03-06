package org.bds.lang.nativeFunctions;

import org.bds.lang.Parameters;
import org.bds.lang.Type;
import org.bds.run.BdsThread;

public class FunctionNativeAssertIntNoMsg extends FunctionNativeAssert {

	public FunctionNativeAssertIntNoMsg() {
		super();
	}

	@Override
	protected void initFunction() {
		functionName = "assert";
		returnType = Type.BOOL;

		String argNames[] = { "expected", "value" };
		Type argTypes[] = { Type.INT, Type.INT };
		parameters = Parameters.get(argTypes, argNames);
		addNativeFunctionToScope();
	}

	@Override
	protected Object runFunctionNative(BdsThread bdsThread) {
		long expected = bdsThread.getInt("expected");
		long value = bdsThread.getInt("value");
		if (expected != value) //
			throw new RuntimeException("Expecting '" + expected + "', but was '" + value + "'.");
		return true;
	}
}
