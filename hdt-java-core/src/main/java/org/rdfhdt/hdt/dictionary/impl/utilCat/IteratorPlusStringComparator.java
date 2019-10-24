/**
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
 *   Dennis Diefenbach:         dennis.diefenbach@univ-st-etienne.fr
 *   Jose Gimenez Garcia:       jose.gimenez.garcia@univ-st-etienne.fr
 */
package org.rdfhdt.hdt.dictionary.impl.utilCat;

import org.rdfhdt.hdt.util.string.CharSequenceComparator;
import java.util.Comparator;

public class IteratorPlusStringComparator implements Comparator<IteratorPlusString> {

    public int compare(IteratorPlusString a, IteratorPlusString b) {
        CharSequenceComparator comparator = new CharSequenceComparator();
        int c = comparator.compare(a.value,b.value);
        if (c>0){
            return 1;
        } else if (c<0) {
            return -1;
        } else {
            return 0;
        }
    }
}