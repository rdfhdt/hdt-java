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

package org.rdfhdt.hdt.triples;

/**
 * TripleID holds a triple as integers
 * 
 */
public class TripleID implements Comparable<TripleID> {

	private int subject;
	private int predicate;
	private int object;

	/**
	 * Basic constructor
	 */
	public TripleID() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param subject
	 *            The subject
	 * @param predicate
	 *            The predicate
	 * @param object
	 *            The object
	 */
	public TripleID(int subject, int predicate, int object) {
		super();
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}
	
	public TripleID(TripleID other) {
		super();
		this.subject = other.subject;
		this.predicate = other.predicate;
		this.object = other.object;
	}

	/**
	 * @return the subject
	 */
	public int getSubject() {
		return subject;
	}

	/**
	 * @param subject
	 *            the subject to set
	 */
	public void setSubject(int subject) {
		this.subject = subject;
	}

	/**
	 * @return the object
	 */
	public int getObject() {
		return object;
	}

	/**
	 * @param object
	 *            the object to set
	 */
	public void setObject(int object) {
		this.object = object;
	}

	/**
	 * @return the predicate
	 */
	public int getPredicate() {
		return predicate;
	}

	/**
	 * @param predicate
	 *            the predicate to set
	 */
	public void setPredicate(int predicate) {
		this.predicate = predicate;
	}

	public void setAll(int subject, int predicate, int object) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "" + subject + " " + predicate + " " + object;
	}
	
	public boolean equals(TripleID other) {
		return !( subject!=other.subject || predicate!=other.predicate || object!=other.object );
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TripleID other) {
		 int result = this.subject - other.subject;

         if(result==0) {
                 result = this.predicate - other.predicate;
                 if(result==0) {
                         return this.object - other.object;
                 } else {
                         return result;
                 }
         } else {
                 return result;
         }
	}
	
	/**
	 * Match a triple to a pattern of TripleID. 0 acts as a wildcard
	 * 
	 * @param pattern
	 *            The pattern to match against
	 * @return boolean
	 */
	public boolean match(TripleID pattern) {

		// get the components of the pattern
		int subjectPattern = pattern.getSubject();
		int predicatePattern = pattern.getPredicate();
		int objectPattern = pattern.getObject();

		/* Remember that 0 acts as a wildcard */
		if (subjectPattern == 0 || this.subject == subjectPattern) {
			if (predicatePattern == 0 || this.predicate == predicatePattern) {
				if (objectPattern == 0 || this.object == objectPattern) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void replace(TripleID replacement) {
		subject = replacement.getSubject();
        object = replacement.getObject();
        predicate = replacement.getPredicate();
	}
	
	public boolean isEmpty() {
		return !(subject != 0 || predicate != 0 || object != 0);
	}

	public boolean isValid() {
		return subject!=0 && predicate!=0 && object!=0;
	}
	
	public String getPatternString() {
		return "" +
			(subject==0   ? '?' : 'S') + 
			(predicate==0 ? '?' : 'P') +
			(object==0    ? '?' : 'O');
	}
	
	public void clear() {
		subject = predicate = object = 0;
	}

	/**
	 * @param other
	 */
	public void set(TripleID other) {
		this.subject = other.subject;
		this.predicate = other.predicate;
		this.object = other.object;
	}
	
}
