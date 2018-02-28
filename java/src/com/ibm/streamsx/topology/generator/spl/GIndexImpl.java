package com.ibm.streamsx.topology.generator.spl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;

public class GIndexImpl implements GIndex {
    
    private JsonObject graph;
    private Map<String, JsonObject> downstreamFromOp= new HashMap<>();
    private Map<String, JsonObject> upstreamFromOp= new HashMap<>();
    
    GIndexImpl(JsonObject graph){
        this.graph = graph;
        
        // Build indexes
        GraphUtilities.operators(graph, op -> {
            Set<JsonObject> ds = GraphUtilities.getDownstream(op, graph);
            Set<JsonObject> us = GraphUtilities.getUpstream(op, graph);
            
            ds.forEach(us_op -> {
                
            });
        });
    }

    @Override
    public JsonObject getGraph() {
        return this.graph;
    }

    @Override
    public Set<JsonObject> getUpstream(JsonObject op) {
        return GraphUtilities.getUpstream(op, graph);
    }
    
    @Override
    public Set<JsonObject> getDownstream(JsonObject op) {
        return GraphUtilities.getDownstream(op, graph);
    }

}
