/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/util/Statistics.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
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

public class Statistics {
	double maxObservedValue;
	double minObservedValue;
	double maxVal; // For intervals
	double total;
	long totalLong;
	long number;
	double meanAccum;
	double deviationAccum;
	
	public Statistics() {
		reset();
	}

	public void reset() {
		number=0;
		minObservedValue=Double.MAX_VALUE;
		maxObservedValue=-Double.MAX_VALUE;
		total=0;
		totalLong=0;
		meanAccum=0;
		deviationAccum=0;
	}
	
	public void process(long val) {
		totalLong+=val;
		this.process((double)val);
	}
	
	public void process(double val) {
		if(Double.isInfinite(val)||Double.isNaN(val)) {
			return;
		}
		
		number++;
		minObservedValue = Math.min(minObservedValue, val);
		maxObservedValue = Math.max(maxObservedValue, val);
		total+=val;
		meanAccum+=val;
		deviationAccum += val * val;
	}

	public void dump(PrintStream out) {
		out.println("# Number: "+getNumber());
		out.println("# Min: "+getMinvalue());
		out.println("# Max: "+getMaxvalue());
		out.println("# Mean: "+getAverage());
		out.println("# Deviation: "+getDeviation());
		out.println("# Total: "+total);
	}

	public double getMaxvalue() {
		return maxObservedValue;
	}

	public double getMinvalue() {
		return minObservedValue;
	}
	
	public double getAverage() {
		return meanAccum/number;
	}
	
	public double getDeviation() {
		double average = getAverage();
		double deviation = deviationAccum/number - average * average;
		deviation = Math.sqrt(deviation);
		return deviation;
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
}
