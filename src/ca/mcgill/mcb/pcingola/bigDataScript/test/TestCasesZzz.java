package ca.mcgill.mcb.pcingola.bigDataScript.test;

import org.junit.Test;

/**
 * Quick test cases when creating a new feature...
 *
 * @author pcingola
 *
 */
public class TestCasesZzz extends TestCasesBase {

	@Test
	public void test11() {
		// Run pipeline and test checkpoint
		runAndCheckpoint("test/checkpoint_11.bds", "test/checkpoint_11.chp", "sumPar", "110");
	}

}
