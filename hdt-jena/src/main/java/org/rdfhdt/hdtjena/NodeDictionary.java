/**
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-jena/src/org/rdfhdt/hdtjena/NodeDictionary.java $
 * Revision: $Rev: 197 $
 * Last modified: $Date: 2013-04-12 19:39:33 +0100 (vie, 12 abr 2013) $
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

package org.rdfhdt.hdtjena;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.rdf.parsers.JenaNodeCreator;
import org.rdfhdt.hdt.rdf.parsers.JenaNodeFormatter;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdtjena.bindings.HDTId;
import org.rdfhdt.hdtjena.cache.DictionaryCache;
import org.rdfhdt.hdtjena.cache.DictionaryCacheArray;
import org.rdfhdt.hdtjena.cache.DictionaryCacheLRI;

/**
 * Wraps all operations from ids to Nodes and vice versa using an HDT Dictionary.
 * 
 * @author mario.arias
 *
 */
public class NodeDictionary {

	private final Dictionary dictionary;

	private final DictionaryCache[] cacheIDtoNode;
	
	public NodeDictionary(Dictionary dictionary) {
		this.dictionary = dictionary;
		
		// ID TO NODE	
		this.cacheIDtoNode = new DictionaryCache[]{
				createCache(dictionary.getNsubjects()),
				createCache(dictionary.getNpredicates()),
				createCache(dictionary.getNobjects()),
		};
	}

	private DictionaryCache createCache(long maxElements) {
		final int idToNodeSize = 20000;
		if(maxElements > idToNodeSize) {
			return new DictionaryCacheLRI(idToNodeSize);
		} else {
			return new DictionaryCacheArray((int) maxElements);
		}
	}

	public Node getNode(int id, TripleComponentRole role) {
		Node node = cacheIDtoNode[role.ordinal()].get(id);
		if(node==null) {
			CharSequence str = dictionary.idToString(id, role);

			node = JenaNodeCreator.create(str);

			cacheIDtoNode[role.ordinal()].put(id, node);
		}

		return node;
	}
	
	public int getIntID(Node node, TripleComponentRole role) {
		return getIntID(nodeToStr(node), role);
	}
	
	public int getIntID(Node node, PrefixMapping map, TripleComponentRole role) {
		return getIntID(nodeToStr(node, map), role);
	}
	
	public int getIntID(CharSequence str, TripleComponentRole role) {
		return dictionary.stringToId(str, role);
	}
	
	public static String nodeToStr(Node node, PrefixMapping map) {
		if(node.isURI()) {
			return map.expandPrefix(node.getURI());
		} else {
			return nodeToStr(node);
		}
	}
	
	public static String nodeToStr(Node node) {
		if(node==null || node.isVariable()) {
			return "";
		} else {
			return JenaNodeFormatter.format(node);
		}
	}

	public TripleID getTripleID(Triple triple, PrefixMapping map) {
		return new TripleID(
				getIntID(nodeToStr(triple.getSubject(), map), TripleComponentRole.SUBJECT),
				getIntID(nodeToStr(triple.getPredicate(), map), TripleComponentRole.PREDICATE),
				getIntID(nodeToStr(triple.getObject(), map), TripleComponentRole.OBJECT)
				);
	}

	public TripleID getTriplePatID(Triple jenaTriple) {
		int subject=0, predicate=0, object=0;
		
		if(jenaTriple.getMatchSubject()!=null) {
			subject = getIntID(jenaTriple.getMatchSubject(), TripleComponentRole.SUBJECT);
		}

		if(jenaTriple.getMatchPredicate()!=null) {
			predicate = getIntID(jenaTriple.getMatchPredicate(), TripleComponentRole.PREDICATE);
		}

		if(jenaTriple.getMatchObject()!=null) {
			object = getIntID(jenaTriple.getMatchObject(), TripleComponentRole.OBJECT);
		}
		return new TripleID(subject, predicate, object);
	}
	
	public static PrefixMapping getMapping(ExecutionContext ctx) {
		Query query = (Query) ctx.getContext().get(ARQConstants.sysCurrentQuery);		
		return query.getPrefixMapping();
	}

    public static Var asVar(Node node)
    {
        if ( Var.isVar(node) )
            return Var.alloc(node) ;
        return null ;
    }

	public static int translate(NodeDictionary dictionary, HDTId id, TripleComponentRole role) {
		if (dictionary == id.getDictionary()) {
			if (role == id.getRole()) {
				return id.getValue();
			} else if (isSO(role) && isSO(id.getRole())) {
				return id.getValue() <= dictionary.dictionary.getNshared() ? id.getValue() : -1;
			}
		}
		
		NodeDictionary dict2 = id.getDictionary();
		
		CharSequence str = dict2.dictionary.idToString(id.getValue(), id.getRole());
		return dictionary.getIntID(str, role);
	}

	private static boolean isSO(TripleComponentRole role) {
		return role == TripleComponentRole.SUBJECT || role == TripleComponentRole.OBJECT;
	}
}
