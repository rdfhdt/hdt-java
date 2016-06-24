/**
 * File: $HeadURL$
 * Revision: $Rev$
 * Last modified: $Date$
 * Last modified by: $Author$
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */

package org.rdfhdt.hdt.util;

import java.io.PrintStream;


public class Histogram {
	int[] data;
	int numBands;
	double maxObservedValue;
	double minObservedValue;
	double maxVal; // For intervals
	double total;
	long totalLong;
	long number;
	double mean;
	double deviation;
	double fisher;
	
	public Histogram(double maxVal, int numBands) {
		this.numBands= numBands;
		reset();
		this.maxVal = maxVal;
	}

	public void reset() {
		data = new int[numBands];
		
		number=0;
		minObservedValue=Double.MAX_VALUE;
		maxObservedValue=-Double.MAX_VALUE;
		total=0;
		totalLong=0;
		mean=0;
		deviation=0;
		fisher=0;
	}
	
	public void process(long val) {
		totalLong+=val;
		this.process((double)val);
	}
	
	public void process(double val) {
		number++;
		minObservedValue = Math.min(minObservedValue, val);
		maxObservedValue = Math.max(maxObservedValue, val);
		total+=val;
		mean+=val;
		deviation += val * val;
		fisher += val * val * val;
		
//		int pos = (int) val;
		int pos = (int)(val*(numBands-1)/maxVal);
		
		if(pos<0) 
			pos=0;
		if(pos>=data.length) 
			pos = data.length-1;

		data[pos]++;
	}
	
	public void end() {
		mean = mean/number;
		deviation = deviation/number - mean * mean;
		deviation = Math.sqrt(deviation);
		
		fisher = fisher/number - mean * mean * mean;
		fisher = fisher/ deviation*deviation*deviation;

	}

	public void dump(PrintStream out) {
		end();
		
		for(int i=0;i<=maxObservedValue && i<data.length;i++) {
			out.println(i+" "+data[i]+" ("+StringUtil.getPercent(data[i], number)+")");
		}
		
		out.println("# Number: "+number++);
		out.println("# Min: "+minObservedValue);
		out.println("# Max: "+maxObservedValue);
		out.println("# Mean: "+mean);
		out.println("# Deviation: "+deviation);
		out.println("# Total: "+total);
	}
	
	public int getNumBands() {
		return numBands;
	}

	public double getMaxvalue() {
		return maxObservedValue;
	}

	public double getMinvalue() {
		return minObservedValue;
	}
	
	public double getAverage() {
		return mean;
	}
	
	public double getDeviation() {
		return deviation;
	}
	
	public double getFisher() {
		return fisher;
	}
	
	public double getTotal() {
		return total;
	}
	
	public long getTotalLong() {
		return totalLong;
	}
	
	public long getNumber() {
		return number;
	}
	
	public int getValue(int num) {
		return data[num];
	}
}
