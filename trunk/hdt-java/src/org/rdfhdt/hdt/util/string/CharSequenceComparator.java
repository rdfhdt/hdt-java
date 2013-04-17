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

package org.rdfhdt.hdt.util.string;

import java.util.Comparator;

/**
 * @author mario.arias
 *
 */
public class CharSequenceComparator implements Comparator<CharSequence> {

	public static final CharSequenceComparator instance = new CharSequenceComparator();
	
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(CharSequence s1, CharSequence s2) {
		if(s1==s2) {
			return 0;
		}
		if(s1 instanceof CompactString && s2 instanceof CompactString) {
			CompactString cs1 = (CompactString) s1;
			CompactString cs2 = (CompactString) s2;
			return cs1.compareTo(cs2);
		}
		
		//TODO ... special for String class like for CompactString above
		
		int len1 = s1.length();
        int len2 = s2.length();
        int n = Math.min(len1, len2);

        int k = 0;
        while (k < n) {
            char c1 = s1.charAt(k);
            char c2 = s2.charAt(k);
            if (c1 != c2) {
                return c2 - c1;
            }
            k++;
        }
        return len2 - len1;
	}

}
