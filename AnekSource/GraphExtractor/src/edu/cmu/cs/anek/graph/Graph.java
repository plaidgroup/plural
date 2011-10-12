package edu.cmu.cs.anek.graph;

import javax.xml.bind.annotation.XmlRootElement;

import edu.cmu.cs.anek.graph.permissions.StateHierarchy;

/**
 * The graph. Holds a collection of method graphs.
 * @author Nels E. Beckman
 *
 */
@XmlRootElement
public final class Graph {
 
    private final Iterable<MethodGraph> methodGraphs;
    private final Iterable<StateHierarchy> stateHierarchies;
    
	public Graph(Iterable<MethodGraph> methodGraphs, Iterable<StateHierarchy> stateHierarchies) {
		this.methodGraphs = methodGraphs;
		this.stateHierarchies = stateHierarchies;
	}

    public Iterable<MethodGraph> getMethods() {
        return methodGraphs;
    }
    
    public Iterable<StateHierarchy> getHierarchies() {
        return this.stateHierarchies;
    }
	
}
