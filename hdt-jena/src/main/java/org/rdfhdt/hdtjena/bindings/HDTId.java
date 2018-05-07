package org.rdfhdt.hdtjena.bindings;

import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdtjena.NodeDictionary;

import org.apache.jena.graph.Node;

public class HDTId {
	
	private final int id;
	private final TripleComponentRole role;	// SUBJECT / PREDICATE / OBJECT
	private final NodeDictionary dict;
	private Node node;		// Caches the associated Node

	public HDTId(int id, TripleComponentRole role, NodeDictionary dict) {
		this.id = id;
		this.role = role;
		this.dict = dict;
	}

	/** "Does not exist" id.  Guaranteed to be not equal to any other HDTId. */
	public HDTId(Node node) {
		this(-1, null, null);
		this.node = node;
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
		if(role == null) {
			// Note: role=null means it does not belong to the HDT
			return null;
		}
		if (node == null) {
			node = dict.getNode(id, role);
		}
		return node;
	}

	public boolean exists() {
		return id != -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof HDTId)) {
			return false;
		}
		HDTId hdtId = (HDTId) obj;
		return exists() && hdtId.exists() &&
				id == NodeDictionary.translate(dict, hdtId, role);
	}
}