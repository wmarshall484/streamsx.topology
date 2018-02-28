package com.ibm.streamsx.topology.generator.spl;

import java.util.Set;

/**
 * The GCompositeDef represents the definition of an SPL composite.
 * It contains methods for accessing and modifying elements of a 
 * JsonObject graph, and encapsulate other objects with the graph
 * such as search indexes.
 *
 */
public interface GCompositeDef extends GIndex{
    
    /**
     * Returns the global index.
     * @return The global index.
     */
    GIndex getGlobalIndex();

}
