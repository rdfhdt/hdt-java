package org.rdfhdt.hdtjena.bindings;

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdtjena.NodeDictionary;

import org.apache.jena.graph.Node;

public class HDTId {
	
	private final int id;
	private final TripleComponentRole role;	// SUBJECT / PREDICATE / OBJECT
	private Node node;		// Caches the associated Node
	private NodeDictionary dict;
	
	public HDTId(int id, TripleComponentRole role, NodeDictionary dict) {
		super();
		this.id = id;
		this.role = role;
		this.node = null;
		this.dict = dict;
	}
	
	public NodeDictionary getDictionary() {
		return dict;
	}
	
	public int getValue() {
		return id;
	}
	
	public TripleComponentRole getRole() {
		return role;
	}
	
	@Override
	public String toString() {
		return "("+id+"/"+role+")";
	}
	
	public Node getNode() {
		return node;
	}
	
	public void setNode(Node n) {
		this.node = n;
	}

	public int getId() {
		return id;
	}
}