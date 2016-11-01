/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/com/hp/hpl/jena/graph/JenaNodeCreator.java $
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

package org.rdfhdt.hdt.rdf.parsers;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 * @author mario.arias
 *
 * Creates Jena Nodes from HDT CharSequences formatted by {@link JenaNodeFormatter}.
 *
 */
public class JenaNodeCreator {

	private JenaNodeCreator() {}

    public static Node create(CharSequence x) {
        if (x.length() == 0) {
            return createURI("");
        }

        char firstChar = x.charAt(0);

        if (firstChar == '_') {
            return createAnon(x);
        } else if (firstChar == '"') {
            return createLiteral(x);
        } else {
            return createURI(x);
        }
    }

    private static Node createAnon(CharSequence x) {
        return NodeFactory.createBlankNode(x.toString().substring(2));
	}

    private static Node createLiteral(CharSequence x) {
		// FIXME: Avoid converting to String?
		String str = x.toString();
		int len=str.length();

		String literal;
		String datatype;
		String lang;

		char next = '\0';
        for(int i=len-1;i>0; i--) {
                char cur = str.charAt(i);

                if(cur=='"') {
                	if(next=='@') {
                		literal = str.substring(1, i);
                        lang = str.substring(i+2, len);

                        return NodeFactory.createLiteral(literal, lang);
                	} else {
                		literal = str.substring(1, i);

                        return NodeFactory.createLiteral(literal);
                	}
                } else if(cur=='^' && next=='^' && str.charAt(i-1)=='"') {
                        literal = str.substring(1, i-1);

                        if(str.charAt(i+2)=='<' && str.charAt(len-1)=='>') {
                        	datatype = str.substring(i+3, len-1);
                        } else {
                        	datatype = str.substring(i+2, len);
                        }
                        RDFDatatype rdfDataType = TypeMapper.getInstance().getSafeTypeByName(datatype);
                        return NodeFactory.createLiteral(literal, rdfDataType);
                }

                next=cur;
        }
        // Note: this line is not reached unless the closing double quote is missing
        return NodeFactory.createLiteral(str.substring(1, len-2));
	}

    private static Node createURI(CharSequence x) {
		return NodeFactory.createURI(x.toString());
	}
}
