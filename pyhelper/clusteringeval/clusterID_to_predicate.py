"""
Create preprocessed (augmented) luster labels file.
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
    """
    output: clustering results augmented with graphID and predicate details
    <graphID.json, clusterID, predicate details>
    """
    import os
    import sys
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
    try:
        # e.g. "clustering-longarticles-qald-25k"
        currentWD = sys.argv[1]
    except:
        print "bad <currentWD> argument. Need something like clustering-longarticles-qald-25k \n Exiting..."
    
    graphsHomeDir       = "/home/pilatus/WORK/pred-clust/data/"
    labelsFile          = "/home/pilatus/WORK/pred-clust/data/" + currentWD + "/25k+qald-longarticles-labels"
    clusterResultsDir   = "/home/pilatus/WORK/pred-clust/data/" + currentWD + "/ClusteringResults/"
    targetDir           = "/home/pilatus/WORK/pred-clust/data/" + currentWD + "/ClusteringResultsLabeled/"

    # verify the currentWD argument 
    if not os.path.exists(labelsFile):
        print "bad argument: graph labels file does not exist\n", labelsFile
    if not os.path.exists(clusterResultsDir):
        print "bad argument: <ClusteringResults> dir does not exist\n", clusterResultsDir
    if not os.path.exists(targetDir):
        print "bad argument: <ClusteringResultsLabeled> dir does not exist\n", targetDir
    
    # 1. read file of graph labels: <sequenctial numbering, graphID>
    # 2. separate the id from the sequence number
    # 3. replace the ".graph" ending with ".json"
    # result: list of relative paths to json graph files
    #labelsList = [".json".join([line.split(",")[1].strip().split(".")[0], ""]) for line in open(labelsFile, "r").readlines()]
    labelsList = [".json".join([line.strip().split(".")[0], ""]) for line in open(labelsFile, "r").readlines()]
    
    resultFilesList = os.listdir(clusterResultsDir)
    pbar = Bar("progress", max=len(resultFilesList))
    

    # for each metric in source directory clusterResultsDir:
    for clusterFile in resultFilesList:
        #print clusterFile
        # put the result file of the same name as the source file into the result folder
        targetFile = codecs.open(targetDir + clusterFile, "w", "utf-8")
        
        clusterids = [line.strip() for line in open(clusterResultsDir + clusterFile, "r").readlines()]
        if len(clusterids) != len(labelsList): 
            print "ERROR: unequal files length: ", clusterFile
        else:
            for i in range(len(clusterids)):
                inputfile = open(graphsHomeDir + labelsList[i], "r")        # read json graph
                graph = json.load(inputfile)
                inputfile.close()
                
                # concat line of <graphID, clusterID, predicate details>
                newline = ",".join([labelsList[i].split("/")[-1], clusterids[i]] + getPredicateDetails(graph))
                targetFile.write(newline + "\n")  
            #___ for each line in cluster ids file 
            
            targetFile.close()
        pbar.next()
    #___ for each cluster results file (metric)
    
    pbar.finish()
        
        
        
        
        
        
        
        
        
        
        
         
        