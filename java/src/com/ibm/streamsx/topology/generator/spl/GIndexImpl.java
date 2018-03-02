package com.ibm.streamsx.topology.generator.spl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.ibm.streamsx.topology.internal.gson.GsonUtilities;

public class GIndexImpl implements GIndex {
    
    private JsonObject graph;
    private Map<String, Set<JsonObject>> downstreamFromOp = new HashMap<>();
    private Map<String, Set<JsonObject>> upstreamFromOp = new HashMap<>();
    private Map<String, Set<JsonObject>> downstreamFromPort = new HashMap<>();
    private Map<String, Set<JsonObject>> upstreamFromPort = new HashMap<>();
    
    GIndexImpl(JsonObject graph){
        this.graph = graph;
        
        // Build indexes
        // Precalculate the upstream and downstream operators of every operator
        GraphUtilities.operators(graph, op -> {
            setUpDownFromOp(op);
        });  
        
    }
   
    // Do a single pass over the graph to determine the upstream and downstream
    // operators from every port and operator.
    private void setUpDownFromOp(JsonObject op){
        Set<JsonObject> ds = GraphUtilities.getDownstream(op, graph);
        Set<JsonObject> us = GraphUtilities.getUpstream(op, graph);
        String key = GsonUtilities.jstring(op, "__unique_id");
        
        // Find the upstream operators from each input port
        GraphUtilities.inputs(op, input -> {
            String portKey = GsonUtilities.jstring(input, "__unique_id");
            Set<JsonObject> iputUs = GraphUtilities.getUpstreamFromPort(input, graph);
            upstreamFromPort.put(portKey, iputUs);
            us.addAll(iputUs);
        });
        
        // Find the downstream operators from each input port
        GraphUtilities.outputs(op, output -> {
            String portKey = GsonUtilities.jstring(output, "__unique_id");
            Set<JsonObject> oportDs = GraphUtilities.getDownstreamFromPort(output, graph);
            downstreamFromPort.put(portKey, oportDs);
            ds.addAll(oportDs);
        });
      
        
        downstreamFromOp.put(key, ds);
        upstreamFromOp.put(key, us);
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
