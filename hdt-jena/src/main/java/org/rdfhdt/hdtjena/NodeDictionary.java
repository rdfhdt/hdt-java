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

import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.rdfhdt.hdt.cache.DictionaryCache;
import org.rdfhdt.hdt.cache.DictionaryCacheArray;
import org.rdfhdt.hdt.cache.DictionaryCacheLRI;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.rdf.parsers.JenaNodeCreator;
import org.rdfhdt.hdt.rdf.parsers.JenaNodeFormatter;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdtjena.bindings.HDTId;

/**
 * Wraps all operations from ids to Nodes and vice versa using an HDT Dictionary.
 * 
 * @author mario.arias
 *
 */
public class NodeDictionary {

	private final Dictionary dictionary;

	@SuppressWarnings("unchecked")
	private final DictionaryCache<Node> cacheIDtoNode [] = new DictionaryCache[TripleComponentRole.values().length];
	
	@SuppressWarnings("unchecked")
	Map<String, Integer> cacheNodeToId [] = new Map[TripleComponentRole.values().length];
	
	public NodeDictionary(Dictionary dictionary) {
		this.dictionary = dictionary;
		
		// ID TO NODE	
		final int idToNodeSize = 20000;
		if(dictionary.getNsubjects()>idToNodeSize) {			
			cacheIDtoNode[0] = new DictionaryCacheLRI<Node>(idToNodeSize);
		} else {
			cacheIDtoNode[0] = new DictionaryCacheArray<Node>((int) dictionary.getNsubjects());
		}
		
		if(dictionary.getNpredicates()>idToNodeSize) {
			cacheIDtoNode[1] = new DictionaryCacheLRI<Node>(idToNodeSize);
		} else {	
			cacheIDtoNode[1] = new DictionaryCacheArray<Node>((int) dictionary.getNpredicates());
		}
		
		if(dictionary.getNobjects()>idToNodeSize) {			
			cacheIDtoNode[2] = new DictionaryCacheLRI<Node>(idToNodeSize);
		} else {
			cacheIDtoNode[2] = new DictionaryCacheArray<Node>((int) dictionary.getNobjects());
		}
		
		// NODE TO ID
		// Disabled, it does not make so much impact.
		cacheNodeToId[0] = DummyMap.getInstance();
		cacheNodeToId[1] = DummyMap.getInstance();
		cacheNodeToId[2] = DummyMap.getInstance();
		
//		final int nodeToIDSize = 1000;
//		
//		cacheNodeToId[0] = new LRUCache<String, Integer>(nodeToIDSize);
//		if(dictionary.getNpredicates()>nodeToIDSize) {
//			System.out.println("Predicates LRU");
//			cacheNodeToId[1] = new LRUCache<String, Integer>(nodeToIDSize);
//		} else {
//			System.out.println("Predicates Map");
//			cacheNodeToId[1] = new ConcurrentHashMap<String, Integer>((int) dictionary.getNpredicates());
//		}
//		cacheNodeToId[2] = new LRUCache<String, Integer>(nodeToIDSize);

	}

	public Node getNode(HDTId hdtid) {
		return getNode(hdtid.getValue(), hdtid.getRole());
	}
	
	public Node getNode(int id, TripleComponentRole role) {
		Node node = cacheIDtoNode[role.ordinal()].get(id);
		if(node==null) {
			CharSequence str = dictionary.idToString(id, role);
			char firstChar = str.charAt(0);
			
			if(firstChar=='_') {
				node = JenaNodeCreator.createAnon(str.toString());
			} else if(firstChar=='"') {
				node = JenaNodeCreator.createLiteral(str.toString());
			} else {
				node = JenaNodeCreator.createURI(str.toString());
			}
			
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
	
	public int getIntID(String str, TripleComponentRole role) {
		Integer intValue = cacheNodeToId[role.ordinal()].get(str);
		if(intValue!=null) {
			return intValue.intValue();
		}
		
		int val = dictionary.stringToId(str, role);
		if(val>0) {
			cacheNodeToId[role.ordinal()].put(str, val);
		}
		return val;
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

    public static final Var asVar(Node node)
    {
        if ( Var.isVar(node) )
            return Var.alloc(node) ;
        return null ;
    }

	public static int translate(NodeDictionary dictionary, HDTId id, TripleComponentRole role) {
		if(dictionary==id.getDictionary()) {
			if (role == id.getRole()) {
			return id.getValue();
			} else if (isSO(role) && isSO(id.getRole())) {
				return id.getValue() <= dictionary.dictionary.getNshared() ? id.getValue() : -1;
			}
		}
		
		NodeDictionary dict2 = id.getDictionary();
		
		CharSequence str = dict2.dictionary.idToString(id.getValue(), id.getRole());
		return dictionary.getIntID(str.toString(), role);
	}

	private static boolean isSO(TripleComponentRole role) {
		return role == TripleComponentRole.SUBJECT || role == TripleComponentRole.OBJECT;
	}
}
