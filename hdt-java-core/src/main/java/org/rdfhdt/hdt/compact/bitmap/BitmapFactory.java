/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/compact/bitmap/BitmapFactory.java $
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

package org.rdfhdt.hdt.compact.bitmap;

import java.io.IOException;
import java.io.InputStream;

import org.rdfhdt.hdt.exceptions.IllegalFormatException;

/**
 * @author mario.arias
 *
 */
public class BitmapFactory {
	
	private BitmapFactory() {}
	
	public static final byte TYPE_BITMAP_PLAIN = 1;
	
	public static Bitmap createBitmap(String type) {
		return new Bitmap375();
	}
	
	public static Bitmap createBitmap(InputStream input) throws IOException {
		input.mark(1);
		int value = input.read();
		input.reset();
		if(value==TYPE_BITMAP_PLAIN) {
			return new Bitmap375();
		}
		throw new IllegalFormatException("Implementation not found for Bitmap with code "+value);
	}
}
