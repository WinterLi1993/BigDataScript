package org.bds.test;

import org.bds.util.Gpr;
import org.junit.Test;

/**
 * Quick test cases when creating a new feature...
 *
 * @author pcingola
 *
 */
public class TestCasesZzz extends TestCasesBase {

	@Test
	public void test90() {
		Gpr.debug("Test");
		runAndCheck("test/run_90.bds", "ok", "true");
	}

}
