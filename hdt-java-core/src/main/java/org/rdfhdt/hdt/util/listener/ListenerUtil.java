/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/util/listener/ListenerUtil.java $
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

package org.rdfhdt.hdt.util.listener;

import org.rdfhdt.hdt.listener.ProgressListener;

/**
 * @author mario.arias
 *
 */
public class ListenerUtil {
	
	private ListenerUtil() {}
	
	public static void notify(ProgressListener listener, String message, float value, float total) {
		if(listener!=null) {
			listener.notifyProgress( ((value)*100/total), message);
		}
	}
	
	public static void notifyCond(ProgressListener listener, String message, long value, long total) {
		if(listener!=null && (value%5000==0)) {
			listener.notifyProgress( ((value)*100/total), message);
		}
	}
	
	public static void notifyCond(ProgressListener listener, String message, long counter, float value, float total) {
		if(listener!=null && (counter%100000==0)) {
			listener.notifyProgress( ((value)*100/total), message);
		}
	}
}
