package com.ibm.streamsx.topology.internal.core;

import static com.ibm.streamsx.topology.generator.operator.OpProperties.LANGUAGE;
import static com.ibm.streamsx.topology.generator.operator.OpProperties.LANGUAGE_JAVA;
import static com.ibm.streamsx.topology.generator.operator.OpProperties.MODEL;
import static com.ibm.streamsx.topology.generator.operator.OpProperties.MODEL_FUNCTIONAL;
import static com.ibm.streamsx.topology.internal.gson.GsonUtilities.jstring;

import com.google.gson.JsonObject;
import com.ibm.streamsx.topology.builder.BOperator;

public interface JavaFunctionalOps {
    
    static boolean isFunctional(BOperator op) {
        return LANGUAGE_JAVA.equals(jstring(op._json(), LANGUAGE))
                && MODEL_FUNCTIONAL.equals(jstring(op._json(), MODEL));
    }
    

    
    String NS = "com.ibm.streamsx.topology.functional.java";
    
    String PKG_O = "com.ibm.streamsx.topology.internal.functional.ops.";

    String AGGREGATE = PKG_O + "FunctionAggregate";
    String AGGREGATE_KIND = NS + "::Aggregate";
    
    String CONVERT_SPL = PKG_O + "FunctionConvertToSPL";
    String CONVERT_SPL_KIND = NS + "::ToSPL";
    
    String FILTER_KIND = NS + "::Filter";
    
    String FLAT_MAP_KIND = NS + "::FlatMap";
    
    String HASH_ADDER_KIND = NS + "::HashAdder";
    
    String HASH_REMOVER_KIND = NS + "::HashRemover"; // Technically not a functional op.

    String JOIN_KIND = NS + "::Join";
    
    String MAP_KIND = NS + "::Map";
    
    String PASS_CLASS = PKG_O + "PassThrough";
    String PASS_KIND = NS + "::PassThrough"; // Technically not a functional op.
    
    String PERIODIC_MULTI_SOURCE_KIND = NS + "::FunctionPeriodicSource";
    
    String SPLIT_KIND = NS + "::Split";
    
    
    String PKG = "com.ibm.streamsx.topology.internal.functional.operators.";    
   
    String FOR_EACH_KIND = NS + "::ForEach";
    
    String SOURCE_KIND = NS + "::Source";
    
    static JsonObject kind2Class() {
        final JsonObject kinds = new JsonObject();
        
        kinds.addProperty(AGGREGATE_KIND, PKG_O + "FunctionAggregate");
        
        kinds.addProperty(CONVERT_SPL_KIND, PKG_O + "FunctionConvertToSPL");
        kinds.addProperty(FILTER_KIND, PKG_O + "FunctionFilter");
        
        kinds.addProperty(FLAT_MAP_KIND, PKG_O + "FunctionMultiTransform");
        
        kinds.addProperty(HASH_ADDER_KIND, PKG_O + "HashAdder");      
        kinds.addProperty(HASH_REMOVER_KIND, PKG_O + "HashRemover");

        kinds.addProperty(JOIN_KIND, PKG_O + "FunctionJoin");
        
        kinds.addProperty(MAP_KIND, PKG_O + "FunctionTransform");
        
        kinds.addProperty(PASS_KIND, PASS_CLASS);
        
        kinds.addProperty(PERIODIC_MULTI_SOURCE_KIND, PKG_O + "FunctionPeriodicSource");
        
        kinds.addProperty(SPLIT_KIND, PKG_O + "FunctionSplit");

        
        kinds.addProperty(FOR_EACH_KIND, PKG + "ForEach");
        
        kinds.addProperty(SOURCE_KIND, PKG + "Source");     
        
        return kinds;
    }
}
