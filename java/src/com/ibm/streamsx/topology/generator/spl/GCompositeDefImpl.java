package com.ibm.streamsx.topology.generator.spl;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.streamsx.topology.internal.gson.GsonUtilities;

public class GCompositeDefImpl implements GCompositeDef{
    GIndex globalIndex;
    Set<JsonObject> operators = new HashSet<>();
    
    public GCompositeDefImpl(JsonObject graph){
        this.globalIndex = new GIndexImpl(graph);
        for(JsonElement jso : GsonUtilities.array(graph, "operators")){
            operators.add(jso.getAsJsonObject());
        }
    }
    
    public GCompositeDefImpl(GIndex globalIndex, Set<JsonObject> ops) {
        this.globalIndex = globalIndex;
        this.operators = ops;
    }

    @Override
    public JsonObject getGraph() {
        return this.globalIndex.getGraph();
    }

    @Override
    public Set<JsonObject> getUpstream(JsonObject op) {
        Set<JsonObject> us = this.globalIndex.getUpstream(op);
        
        // Remove operators that are not in the composite.
        us.removeIf(us_op -> !operators.contains(us_op));
        return us;
    }
    
    @Override
    public Set<JsonObject> getDownstream(JsonObject op) {
        Set<JsonObject> ds = this.globalIndex.getDownstream(op);
        
        // Remove operators that are not in the composite.
        ds.removeIf(ds_op -> !operators.contains(ds_op));
        return ds;
    }

    @Override
    public GIndex getGlobalIndex() {
        return this.globalIndex;
    }
}
