package org.rdfhdt.hdt.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.rdfhdt.hdt.iterator.utils.MergedIterator;

import static org.junit.Assert.*;

public class MergedIteratorTest {
	List<Integer> listA, listB, listC;

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
		int[] intsB = {3,4,5};
		Integer[] intsExpected = {1, 3, 4, 5, 6};
		
		listA=getList(intsA);
		listB=getList(intsB);
		listC=Arrays.asList(intsExpected);
	}

	@Test
	public void testOneEmpty() {
		
		Iterator<Integer> it = new MergedIterator<>(listA.iterator(), listB.iterator(), new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });
		
		Iterator<Integer> itE = listC.iterator();
	
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
