package org.rdfhdt.hdt.iterator;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.rdfhdt.hdt.iterator.utils.MultiMerge;
import org.rdfhdt.hdt.iterator.utils.ReducerLeft;
import org.rdfhdt.hdt.iterator.utils.SortedReduceIterator;

public class MultiMergeTest {
	List<Integer> listA, listB, listC, listOut, listOutB;

	private List<Integer> getList(int[] ints) {
	    List<Integer> intList = new ArrayList<>();
	    for (int index = 0; index < ints.length; index++)
	    {
	        intList.add(ints[index]);
	    }
	    return intList;
	}
	
	@Before
	public void setUp() throws Exception {

		int[] intsA = {1, 4, 6};
		int[] intsB = {2, 4,7};
		int[] intsC = {2,4,5, 9, 12};
		Integer[] intsExpected = {1,2,2,4,4,4,5,6,7,9,12 };
		Integer[] intsExpectedB = {1,2,4,5,6,7,9,12 };
		
		listA=getList(intsA);
		listB=getList(intsB);
		listC=getList(intsC);
		listOut=Arrays.asList(intsExpected);
		listOutB=Arrays.asList(intsExpectedB);
	}
	
	private static class IntegerComparator implements Comparator<Integer>{
		@Override
		public int compare(Integer o1, Integer o2) {
			return Integer.compare(o1, o2);
		}
	}
	
	@Test
	public void testCase() {
		
		List<Iterator<Integer>> list = new ArrayList<>();
		
		list.add(listA.iterator());
		list.add(listB.iterator());
		list.add(listC.iterator());
		
		Iterator<Integer> it = new MultiMerge<>(list.iterator(), new IntegerComparator());
		
		it = new SortedReduceIterator<>(it, new ReducerLeft<Integer>());
		
		Iterator<Integer> itE = listOutB.iterator();
	
		while(it.hasNext()) {
			assertEquals(itE.hasNext(), it.hasNext());
			
			int val = it.next();
			int valE = itE.next();
//			System.out.println(val);
			assertEquals(val, valE);
		}
		
		assertEquals(itE.hasNext(), it.hasNext());
	}

}
