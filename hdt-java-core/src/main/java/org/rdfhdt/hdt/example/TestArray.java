
package org.rdfhdt.hdt.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.rdfhdt.hdt.util.MapArray;

public class TestArray {


	public static void main(String[] args) throws Throwable {

		// 1.- test to save an array with some strings
		ArrayList<String> exampleContent = new ArrayList<String>();
		int totalEntries=10000000;
		for (int n = 0; n < totalEntries ; n++) {
			exampleContent.add("This is my string stored in position " + n);
			if (n % 1000000 == 0) {
				System.out.println("Loading " + n + " strings.");
			}
		}
		System.out.println("ALL Loaded! Creating array... ");
		Iterator<String> it = exampleContent.iterator();
		
		// write array
		MapArray writeArray = new MapArray();
		writeArray.write(it, totalEntries, "myArray.txt");
		System.out.println("Array created! ");
		System.out.println("Elements in array: "+writeArray.getNumElements());
		System.out.println("Test Array! ");
		
		// 2.- test to map an array
		MapArray readArray = new MapArray();
		readArray.map("myArray.txt");
		
		//3.- test to get 10 random positions of the array
		for (int i = 0; i < 10; i++) {
			long id = Math.abs(new Random().nextLong()%readArray.getNumElements());

			String mystr = readArray.extract((int)id);
			System.out.println("The string in ID " + id + " is: " + mystr);
		}

	}

}
