/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/compact/bitmap/Bitmap.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
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
	/**
	 * Get the value of the bit at position pos
	 * @param pos
	 * @return
	 */
	boolean access(long pos);
	
	/**
	 * Count the number of ones up to position pos (included)
	 * @param pos
	 * @return
	 */
    long rank1(long pos);
    
    /**
     * Count the number of zeros up to position pos (included)
     * @param pos
     * @return
     */
    long rank0(long pos);
    
    /**
     * Return the position of the next 1 after position start.
     * @param start
     * @return
     */
    long selectPrev1(long start);
    
    /**
     * Return the position of the previous 1 before position start.
     * @param start
     * @return
     */
    long selectNext1(long start);
    
    /**
     * Find the position where n zeros have appeared up to that position.
     * @param x
     * @return
     */
    long select0(long n);
    
    /**
     * Find the position where n ones have appeared up to that position.
     * @param n
     * @return
     */
    long select1(long n);
    
    /**
     * Get number of total bits in the data structure
     * 
     * @return
     */
    long getNumBits();
    
    /**
     * Count the number of total ones in the data structure.
     * @return
     */
    long countOnes();
    
    /** 
     * Count the number of total zeros in the data structure.
     * 
     * @return
     */
    long countZeros();
    
    /**
     * Estimate the size in bytes of the total data structure.
     * @return
     */
    long getSizeBytes();
    
    /**
     * Dump Bitmap into an OutputStream
     * @param output
     * @param listener
     * @throws IOException
     */
	void save(OutputStream output, ProgressListener listener) throws IOException;
	
	/**
	 * Load Bitmap from an InputStream
	 * @param input
	 * @param listener
	 * @throws IOException
	 */
	void load(InputStream input, ProgressListener listener) throws IOException;
	
	/**
	 * Return the type of the data structure as defined in HDTVocabulary
	 * @return
	 */
	String getType();
}
