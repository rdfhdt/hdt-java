package org.rdfhdt.hdtjena.solver;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.ARQInternalErrorException;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.engine.Plan;
import com.hp.hpl.jena.sparql.engine.QueryEngineFactory;
import com.hp.hpl.jena.sparql.engine.QueryEngineRegistry;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.main.QueryEngineMain;
import com.hp.hpl.jena.sparql.util.Context;

public class HDTQueryEngine extends QueryEngineMain {

	protected Query hdtQuery;
	protected DatasetGraph hdtDataset;
	protected Binding hdtBinding;
	protected Context hdtContext;
	
    public HDTQueryEngine(Query query, DatasetGraph dataset, Binding input, Context context)
    { 
        super(query, dataset, input, context) ;
        this.hdtQuery = query;
        this.hdtDataset = dataset;
        this.hdtBinding = input;
        this.hdtContext = context;
    }
	
	public HDTQueryEngine(Op op, DatasetGraph dataset, Binding input, Context context) {
		super(op, dataset, input, context);
	}

	@Override
	protected Plan createPlan() {
		Plan plan = OptimizedCount.getPlan(this, hdtQuery, hdtDataset, hdtBinding, hdtContext);
		if(plan!=null) {
			return plan;
		}
		
		return super.createPlan();
	}
		
    // ---- Registration of the factory for this query engine class. 
    
    // Query engine factory.
    // Call HDTQueryEngine.register() to add to the global query engine registry. 

    static QueryEngineFactory factory = new HDTQueryEngineFactory() ;

    static public QueryEngineFactory getFactory() { 
    	return factory;
    } 
    
    static public void register(){
    	QueryEngineRegistry.addFactory(factory) ; 
    }
    
    static public void unregister(){ 
    	QueryEngineRegistry.removeFactory(factory);
    }

    static class HDTQueryEngineFactory implements QueryEngineFactory {

    	@Override
    	public boolean accept(Query query, DatasetGraph dataset, Context context) {   		
    		return true;
    	}

    	@Override
    	public Plan create(Query query, DatasetGraph dataset, Binding initial, Context context) {
    		HDTQueryEngine engine = new HDTQueryEngine(query, dataset, initial, context);
    		return engine.getPlan();
    	}

    	@Override
    	public boolean accept(Op op, DatasetGraph dataset, Context context) {
    		// Refuse to accept algebra expressions directly.
    		return false;
    	}

    	@Override
    	public Plan create(Op op, DatasetGraph dataset, Binding inputBinding, Context context) {
    		// Should not be called because accept/Op is false
    		throw new ARQInternalErrorException("HDTQueryEngine: factory calleddirectly with an algebra expression");
    	}
    }
}
