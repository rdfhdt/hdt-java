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

package org.rdfhdt.hdt.compact.bitmap;

import org.rdfhdt.hdt.listener.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mario.arias
 *
 */
public interface Bitmap {
	boolean access(long pos);
    long rank1(long pos);
    long rank0(long pos);
    long selectPrev1(long start);
    long selectNext1(long start);
    long select0(long x);
    long select1(long x);
    long getNumBits();
    long countOnes();
    long countZeros();
    long getSizeBytes();
	void save(OutputStream output, ProgressListener listener) throws IOException;
	void load(InputStream input, ProgressListener listener) throws IOException;
	String getType();
}
