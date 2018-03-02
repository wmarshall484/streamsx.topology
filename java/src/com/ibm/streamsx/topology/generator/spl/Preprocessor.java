/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.generator.spl;

import static com.ibm.streamsx.topology.builder.BVirtualMarker.END_PARALLEL;
import static com.ibm.streamsx.topology.builder.BVirtualMarker.ISOLATE;
import static com.ibm.streamsx.topology.builder.BVirtualMarker.PARALLEL;
import static com.ibm.streamsx.topology.generator.spl.GraphUtilities.addBefore;
import static com.ibm.streamsx.topology.generator.spl.GraphUtilities.findOperatorByKind;
import static com.ibm.streamsx.topology.internal.gson.GsonUtilities.jstring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.streamsx.topology.builder.BVirtualMarker;
import com.ibm.streamsx.topology.function.Consumer;
import com.ibm.streamsx.topology.internal.gson.GsonUtilities;

/**
 * Preprocessor modifies the passed in JSON to perform
 * logical graph transformations.
 */
class Preprocessor {
    
    private final SPLGenerator generator;
    private final GCompositeDef gcomp;
    
    Preprocessor(SPLGenerator generator, GCompositeDef gcomp) {
        this.generator = generator;
        this.gcomp = gcomp;
    }
    
    void preprocess() {
        
        GraphValidation graphValidationProcess = new GraphValidation();
        graphValidationProcess.validateGraph(gcomp);
        
        isolateParalleRegions();
        
        PEPlacement pePlacementPreprocess = new PEPlacement(generator, gcomp);

        // The hash adder operators need to be relocated to enable directly 
	// adjacent parallel regions
        // TODO: renable adjacent parallel regions optimization
        //relocateHashAdders();
        
        pePlacementPreprocess.tagIsolationRegions();
        pePlacementPreprocess.tagLowLatencyRegions();
        
        
        
        ThreadingModel.preProcessThreadedPorts(gcomp);
        
        removeRemainingVirtualMarkers();
        
        AutonomousRegions.preprocessAutonomousRegions(gcomp);
        
        pePlacementPreprocess.resolveColocationTags();

        // Optimize phase.
        new Optimizer(gcomp).optimize();       
    }
       
    /**
     * Isolate parallel regions to ensure that
     * we get parallelism through multiple PEs
     * (with the ability to have those PEs be distributed
     * across multiple hosts).
     * 
     * For 4.2 and later we achieve this using deploymentConfig
     * unless there are isolated regions.
     * 
     * Pre-4.2 we insert isolates prior to region and after the region.
     */
    private void isolateParalleRegions() {
        boolean needExplicitIsolates = !generator.versionAtLeast(4, 2);
        
        // TODO 4.2 checking
        
        if (!needExplicitIsolates)
            return;
        
        // Add isolate before the parallel and end parallel markers
        Set<JsonObject> parallelOperators = findOperatorByKind(PARALLEL, gcomp);  
        parallelOperators.addAll(findOperatorByKind(END_PARALLEL, gcomp));
        for (JsonObject po : parallelOperators) {
            String schema = po.get("inputs").getAsJsonArray().get(0).getAsJsonObject().get("type").getAsString();
                        
            addBefore(po, newMarker(schema, ISOLATE), gcomp);         
        }       
    }
    
    
    private int ppMarkerCount;
    /**
     * Create a new marker operator that can be inserted into
     * the graph using addBefore.
     */
    private JsonObject newMarker(String schema, BVirtualMarker marker) {
        JsonObject op = new JsonObject();
        op.addProperty("marker", true);
        op.addProperty("kind", marker.kind());
        String name = "$$PreprocessorMarker_" + ppMarkerCount++;
        op.addProperty("name", name);
        
        {
            JsonArray inputs = new JsonArray();
            op.add("inputs", inputs);
            JsonObject input = new JsonObject();
            inputs.add(input);            
            input.addProperty("index", 0);
            input.addProperty("name", name + "_IN");
            input.addProperty("type", schema);
            input.add("connections", new JsonArray());
            
        }
        {
            JsonArray outputs = new JsonArray();
            op.add("outputs", outputs);
            JsonObject output = new JsonObject();
            outputs.add(output);
            output.addProperty("index", 0);
            output.addProperty("type", schema);
            output.add("connections", new JsonArray());
            output.addProperty("name", name + "_IN");
        }
        return op;
    }
    
    private void removeRemainingVirtualMarkers(){
        for (BVirtualMarker marker : Arrays.asList(BVirtualMarker.UNION, BVirtualMarker.PENDING)) {
            Set<JsonObject> unionOps = GraphUtilities.findOperatorByKind(marker, gcomp);
            GraphUtilities.removeOperators(unionOps, gcomp);
        }
    }
    
    private void assertValidParallelUnions(Set<JsonObject> hashAdders) {   
        for(JsonObject hashAdder : hashAdders){
            Set<JsonObject> hashAdderParents = gcomp.getUpstream(hashAdder);
            Set<JsonObject> tmp = new HashSet<>();
            
            // Add all $Unparallel$ parents of hashAdder to list
            for(JsonObject hashAdderParent : hashAdderParents){
                if(jstring(hashAdderParent, "kind").equals(BVirtualMarker.END_PARALLEL.kind())){
                    tmp.add(hashAdderParent);
                }
            }
            hashAdderParents = tmp;
            
            // Assert that the downstream hashadders of each unparallel 
            // operator are all of the same routing type.
            for(JsonObject hashAdderParent : hashAdderParents){
                String lastRoutingType = null;
                Set<JsonObject> unparallelChildren = gcomp.getDownstream(hashAdderParent);
                for(JsonObject unparallelChild : unparallelChildren){
                    if(jstring(unparallelChild, "kind").equals("com.ibm.streamsx.topology.functional.java::HashAdder")
		       || jstring(unparallelChild, "kind").equals(BVirtualMarker.PARALLEL.kind())) {
                        if(lastRoutingType != null && !(jstring(unparallelChild, "routing")).equals(lastRoutingType)){
                            throw new IllegalStateException("A TStream from an endParallel invocation is being used to begin"
                                    + " two separate parallel regions that have two different kind of routing.");
                        }
                        lastRoutingType = jstring(unparallelChild, "routing");
                    }
                }          
            }           
        }
    }

}
