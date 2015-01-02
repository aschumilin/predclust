"""
Compute statistics given a csv-file of clustering labels + predicate details.
"""
from boto.redshift.exceptions import ClusterAlreadyExists
'''
Created on Dec 8, 2014

@author: Artem
'''
from collections import Counter

def countGraphsPerCluster(clusterIDsList):
    """
    Count for each cluster, how many graphs it has.
    Input: simple raw list of clusterIDs
    Return: a List of tuples [ ( cluster_i , #graphs_in_i ) , (...)]
    """

    graphsCounter = Counter()
    
    for clusterID in clusterIDsList:
        graphsCounter[clusterID] += 1
    
    return graphsCounter.items()

def getClusterPredicateDict(preprocessedClusteringResultsList):
    """
    Create human-readable overview of clusters and their predicates.
    Input: list of csv-strings of the shape <graphID.json, clusterID, predicate details>
    Output: dict with { clusterID : dict of { predicate : count in cluster } }
    e.g. { 998 : { have.01 : 20 } }
    """
    clusterPredicatesDict = dict()
    
    # 1. first pass: preallocate empty lists in dictionary
    for graphLine in preprocessedClusteringResultsList:
        parts = graphLine.split(",")
        clusterID = parts[1]
        clusterPredicatesDict.update({clusterID : Counter() })
    
    # 2. second pass: fill empty lists with graphIDs
    for graphLine in preprocessedClusteringResultsList:
        parts = graphLine.split(",")
        predicate = parts[2]
        clusterID = parts[1]
    
    
        # update predicate dict of this cluster with this predicate
        clusterPredicatesDict.get(clusterID)[predicate] += 1
       
    return clusterPredicatesDict
#___ getClusterPredicateDict()


def getClusterGraphsDict(preprocessedClusteringResultsList):
    """
    Read pre-processed clustering result file and output 
    a dict with clusterIDs and corresponding graphIDs.
    Input: list of csv-strings of the shape <graphID.json, clusterID, predicate details>
    Output: dict with {clusterID : [graphID1, graphID2,...]}
    e.g. {998 : ["es-4261544-108-1", ...]}
    """
    clusterGraphsDict = dict()
    
    # 1. first pass: preallocate empty lists in dictionary
    for graphLine in preprocessedClusteringResultsList:
        parts = graphLine.split(",")
        clusterID = parts[1]
        clusterGraphsDict.update({clusterID : []})
        
        
    # 2. second pass: fill empty lists with graphIDs
    for graphLine in preprocessedClusteringResultsList:
        parts = graphLine.split(",")
        graphID = parts[0]
        clusterID = parts[1]
    
    
        # remove file ending from the graphID
        # append graphID to list in dict    
        if len(graphID.split(".")) > 0:
            clusterGraphsDict.get(clusterID).append(graphID.split(".")[0])
        else: 
            clusterGraphsDict.get(clusterID).append(graphID)
            
    return clusterGraphsDict

def processLines(linesOfFileList):
    """
    Example:
    en-14531824-104-1.json,3.8000000e+01,manifest.01,evidenced
    Return each column as a list.
    """
    separator = ","
    graphIDs, clusterIDs, predIDs, mentions = [],[],[],[]
    
    for line in linesOfFileList:
        parts = line.split(separator)
        graphIDs.append(parts[0])
        clusterIDs.append(parts[1])
        predIDs.append(parts[2])
        mentions.append(parts[3])
    #___ iterate over lines in cluster labels file
    
    return [graphIDs, clusterIDs, predIDs, mentions]
    

def isSpanishGraph(graphID):
    """
    determine by graphID if graph is spanish or english
    """
    return graphID[1] is "s"

    
def spanishPerCluster(clusterIDsList, graphIDsList, clustersAndTotalCounts=None):
    """
    calculate the spanish percentage in each cluster
    input: clustersAndTotalCounts = list of tuples [(clusterID, count), ()]
    return list of tuples [(clusterID, %ES in cluster), ()]
    """
    
    if clustersAndTotalCounts is None:
        clustersAndTotalCounts = countGraphsPerCluster(clusterIDsList)
    
    esPerClusterCounter = Counter()
    
    # iterate over all graphs IDs and count the spanish graphs in each cluster
    for i in range(len(clusterIDsList)):
        esPerClusterCounter[clusterIDsList[i]] += 0 # init zero value in case this cluster has 0 ES graphs
        
        if isSpanishGraph(graphIDsList[i]):
            esPerClusterCounter[clusterIDsList[i]] += 1
    #___ done counting
    
    # compose return data structure
    returnListOfTuples = []
    for tupl in clustersAndTotalCounts:
        esPercentage = 100.0 * esPerClusterCounter.get(tupl[0]) / tupl[1]
        returnListOfTuples.append( (tupl[0], esPercentage ) )     
    #___ done writing list of result tuples  
    return returnListOfTuples    
   
   
def saveHistogram(dataRow, numBins, xlab, ylab, title, filename): 
    import matplotlib.pyplot
    
    
    n, bins, patches = matplotlib.pyplot.hist(dataRow, numBins, normed=0, facecolor='blue')#, alpha=0.5)

    matplotlib.pyplot.xlabel(xlab)
    matplotlib.pyplot.ylabel(ylab)
    matplotlib.pyplot.title(title)
    
    # Tweak spacing to prevent clipping of ylabel
    #plt.subplots_adjust(left=0.15)
    matplotlib.pyplot.savefig(filename, bbox_inches='tight')
    # clear current figure !!!
    matplotlib.pyplot.clf()
    
    

    
if __name__ == '__main__':
    import sys
    import codecs
    from util.toCSV import listToCSV
    
    labels = []
    try:
        labelsFile = sys.argv[1]
        labels = codecs.open(labelsFile, "r").readlines()
    except:
        print "ERROR in cluster-stats.main: bad argument. Exiting..."
        exit
    
    
    # write human-readable file of clusters and their predicates
    clustersPredicates = getClusterPredicateDict(labels)
    for clusterID in clustersPredicates:
        print clusterID, " ---------------------------------"
        predsDict = clustersPredicates.get(clusterID)
        for pred in predsDict:
            print "\t", predsDict.get(pred), "\t", pred


    """
    graphIDs, clusterIDs, predIDs, mentions = processLines(labels)
    
    #  count graphs per cluster
    clustersAndCounts = countGraphsPerCluster(clusterIDs)
    print "cluster counts ", len(clustersAndCounts)
    listToCSV(labelsFile + ".counts", clustersAndCounts, "cluster_ID,graphs_count")
    
    
    # count en-es mixture in each cluster
    clustersAndEsPercents = spanishPerCluster(clusterIDs, graphIDs) 
    print "percentages ", len(clustersAndEsPercents)
    listToCSV(labelsFile + ".es-percent", clustersAndEsPercents, "cluster_ID,%_spanish_predicates")
    
    # plot histograms
    plotTitle = labelsFile.split("/")[-1].split(".")[0]
    saveHistogram([tupl[1] for tupl in clustersAndCounts], 100, "# graphs in cluster", "# of clusters", plotTitle, labelsFile+".graphs-per-cluster.png")

    
    saveHistogram([tupl[1] for tupl in clustersAndEsPercents], 100, "% of spanish graphs in cluster", "# of clusters", plotTitle, labelsFile+".es-percent.png")
    """
    

