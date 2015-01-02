'''
Created on Dec 31, 2014

@author: Artem
'''
import json
from collections import Counter

if __name__ == '__main__':
    
    gListFileName = "/home/pilatus/WORK/pred-clust/data/clustering-shorts-qald-25k/25k+qald-shortarticles-labels"
    gBaseDir = "/home/pilatus/WORK/pred-clust/data/"
    gPaths = [line.strip().replace(".graph", ".json") for line in open(gListFileName,  "r").readlines()]
    
    dbpEntsCounter = 0
    uniqueEntsCounter = Counter()
    
    for graphPath in gPaths:
        gFile = open(gBaseDir + graphPath, "r")
        graph = json.load(gFile)
        
        args = graph.get("arguments")
        
        # count dbp entities in all args of graph
        for arg in args:
            argNode = arg.get("node")
            if argNode:
                argRefs = argNode.get("references")
            else:
                continue # predicate has no node args
            
                # scan only top level arguments
            if len(argRefs) > 0 :
                # scan all references of that node 
                for ref in argRefs:
                    # print only dbpedia ents
                    #print ref
                    if str(ref.get("knowledgeBase"))[0] is "d":
                        dbpEntsCounter += 1
                        uniqueEntsCounter[ref.get("URI")] += 1
                        #print ref.get("URI")
            #===================================================================
            # try:
            #     argRefs =  arg.get("node").get("references")
            #     # scan only top level arguments
            #     if ( argRefs is not None ) and ( len(argRefs) > 0 ):
            #         # scan all references of that node 
            #         for ref in argRefs:
            #             # print only dbpedia ents
            #             if ref.get("knowledgeBase")[0] is "d":
            #                 print ref.get("URI")
            # except:
            #     print "-------------------"
            #     print arg
            #     print "-------------------"
            #===================================================================
            
                
        #___ loop over all arguments of graph
        
    #___ loop over all graphs
    print "all ents long: ", str(dbpEntsCounter)
    print "unique ents long: ", str(len(uniqueEntsCounter))