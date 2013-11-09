package org.rdfhdt.hdt.iterator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class MergedIteratorTest {
	List<Integer> listA, listB;

	private List<Integer> getList(int[] ints) {
	    List<Integer> intList = new ArrayList<Integer>();
	    for (int index = 0; index < ints.length; index++)
	    {
	        intList.add(ints[index]);
	    }
	    return intList;
	}
	
	@Before
	public void setUp() throws Exception {

		int[] intsA = {1, 4, 6};
		int[] intsB = {3,4,5};
		
		listA=getList(intsA);
		listB=getList(intsB);
	}

	@Test
	public void testOneEmpty() {
		
		Iterator<Integer> it = new MergedIterator<Integer>(listA.iterator(), listB.iterator(), new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return o2.compareTo(o1);
			}
		});
	
		while(it.hasNext()) {
			Integer val = it.next();
			System.out.println(val);
		}
	}

}
