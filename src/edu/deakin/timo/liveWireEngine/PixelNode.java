//this class was created to store pixel nodes in a Priority Queue
//so that Dijkstra can run on O(n log n)
//The interface Comparable is required so that the Java class PriorityQueue
//could be used
//
package edu.deakin.timo.liveWireEngine;

class PixelNode implements Comparable<PixelNode> {
    private int[] myIndex;
    private double myDistance;
    private int[] whereFrom;
    public PixelNode(int[] index, double distance, int[] whereFrom){
	myIndex = index;
	myDistance = distance;
	this.whereFrom = whereFrom;
    }
    public double getDistance(){
	return myDistance;
    }
    public int[] getIndex(){
	return myIndex;
    }
    public int[] getWhereFrom(){
	return whereFrom;
    }

    public int compareTo(PixelNode other){
	if( myDistance < other.getDistance()) 
	    return -1;
	else if( myDistance > other.getDistance()) 
	    return +1;
	else 
	    return 0;
	//	return (int)((myDistance - other.getDistance()));//plus 0.5 to round
    }

}
