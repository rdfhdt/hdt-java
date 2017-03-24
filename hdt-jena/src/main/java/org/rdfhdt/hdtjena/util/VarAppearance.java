/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/util/VarAppearance.java $
 * Revision: $Rev: 190 $
 * Last modified: $Date: 2013-03-03 11:30:03 +0000 (dom, 03 mar 2013) $
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
 */

package org.rdfhdt.hdtjena.util;

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;

public final class VarAppearance {
	private byte value;
	private static final byte SUBJECT = 1;
	private static final byte PREDICATE = 2;
	private static final byte OBJECT = 4;
	private static final byte SUBJ_OBJ = SUBJECT | OBJECT;
		
	public void setSubject() {
		value |= SUBJECT;
	}
	public void setPredicate() {
		value |= PREDICATE;
	}
	public void setObject() {
		value |= OBJECT;
	}
	
	public boolean isSubject() {
		return (SUBJECT & value)!=0;
	}
	public boolean isPredicate() {
		return (PREDICATE & value)!=0;
	}
	public boolean isObject() {
		return (OBJECT & value)!=0;
	}
	
	public boolean isSubjectObject() {
		return (SUBJ_OBJ & value)==SUBJ_OBJ;
	}
	
	public TripleComponentRole getRole() throws NotFoundException {
		if( (SUBJECT & value)!=0 ) {
			return TripleComponentRole.SUBJECT;
		}
		if((PREDICATE & value)!=0) {
			return TripleComponentRole.PREDICATE;
		}
		if((OBJECT & value)!=0) {
			return TripleComponentRole.OBJECT;
		}
		throw new NotFoundException("No role set.");
	}
	
	public void set(TripleComponentRole role) {
		switch (role) {
		case SUBJECT:
			value |= SUBJECT;
			break;
		case PREDICATE:
			value |= PREDICATE;
			break;
		case OBJECT:
			value |= OBJECT;
			break;
		}
	}
}
