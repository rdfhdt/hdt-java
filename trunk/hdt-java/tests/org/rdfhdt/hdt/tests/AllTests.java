package org.rdfhdt.hdt.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.rdfhdt.hdt.compact.array.IntegerArrayTest;
import org.rdfhdt.hdt.compact.array.LongArrayTest;
import org.rdfhdt.hdt.compact.bitmap.BitSequence375Test;
import org.rdfhdt.hdt.compact.integer.VByteTest;
import org.rdfhdt.hdt.util.io.IOUtilTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	IntegerArrayTest.class,
	LongArrayTest.class,
	IntegerArrayTest.class,
	BitSequence375Test.class,
	IOUtilTest.class,
	VByteTest.class
})
public class AllTests {
}
