"""
Read the file of cluster IDs + graph list. 
Output the cluster IDs along with graphID, 
predicate surface form etc.
"""
'''
Created on Dec 8, 2014

@author: Artem
'''

def getPredicateDetails(graph):
                
    predicate = graph.get("predicate")
    return [predicate.get("displayName").strip(), predicate.get("mention").strip()]


if __name__ == '__main__':
    import os
    import json
    from progress.bar import Bar
    import codecs
    #===========================================================================
    # read clusterID list
    # read graphs list
    # for each graph in list:
    #     get predicate details
    #     print graphID, clusterID, predID, surface form, wordnetID
    # 
    #===========================================================================
    
    graphsHomeDir       = "/home/pilatus/WORK/pred-clust/data/remus-backup/"
    labelsFile          = "/home/pilatus/WORK/pred-clust/data/remus-backup/25k-lab"
    clusterResultsDir   = "/home/pilatus/WORK/pred-clust/data/remus-backup/ClusteringResults/"
    targetDir           = "/home/pilatus/WORK/pred-clust/data/remus-backup/ClusteringResultsLabeled/"

     
    labelsList = [".json".join([line.split(",")[1].strip().split(".")[0], ""]) for line in open(labelsFile, "r").readlines()]
    
    resultFilesList = os.listdir(clusterResultsDir)
    pbar = Bar("progress", max=len(resultFilesList))
    


    for clusterFile in resultFilesList:
        
        targetFile = codecs.open(targetDir + clusterFile, "w", "utf-8")
        clusterids = [line.strip() for line in open(clusterResultsDir + clusterFile, "r").readlines()]
        if len(clusterids) != len(labelsList): 
            print "ERROR: unequal files length: ", clusterFile
        else:
            for i in range(len(clusterids)):
                inputfile = open(graphsHomeDir + labelsList[i], "r")
                graph = json.load(inputfile)
                inputfile.close()
                newline = ",".join([labelsList[i].split("/")[-1], clusterids[i]] + getPredicateDetails(graph))
                targetFile.write(newline + "\n")  
            #___ for each line in cluster ids file 
            
            targetFile.close()
        pbar.next()
    #___ for each cluster results file
    pbar.finish()
        
        
        
        
        
        
        
        
        
        
        
         
        