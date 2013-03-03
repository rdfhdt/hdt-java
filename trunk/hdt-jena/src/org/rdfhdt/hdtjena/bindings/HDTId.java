package org.rdfhdt.hdtjena.bindings;

import org.rdfhdt.hdt.enums.TripleComponentRole;

import com.hp.hpl.jena.graph.Node;

public class HDTId {
	
	private final int id;
	private final TripleComponentRole role;	// SUBJECT / PREDICATE / OBJECT
	private Node node;		// Caches the associated Node
	
	public HDTId(int id, TripleComponentRole role) {
		super();
		this.id = id;
		this.role = role;
		this.node = null;
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
