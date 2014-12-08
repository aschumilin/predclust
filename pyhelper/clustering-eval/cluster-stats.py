"""
Compute statistics given a csv-file of clustering labels + predicate details.
"""
'''
Created on Dec 8, 2014

@author: Artem
'''
from itertools import groupby

def graphsPerCluster(clusterIDsList):
    """
    Count for each cluster, how many graphs it has.
    Input: simple raw list of clusterIDs
    Return: a List of tuples [ ( cluster_i , #graphs_in_i ) , (...)]
    """
        
    return [(clusteriD, clusterIDsList.count(clusteriD)) for clusteriD,_ in groupby(clusterIDsList)]


    


def processLines(linesOfFileList):
    """
    Example:
    en-14531824-104-1.json,3.8000000e+01,manifest.01,evidenced
    Return each column as a list.
    """
    separator = ","
    graphIDs, clusterIDs, predIDs, mentions = []
    
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
    calculate the language mixture in each cluster
    input: clustersAndTotalCounts = list of tuples [(clusterID, count), ()]
    return list of tuples [(clusterID, %ES in cluster), ()]
    """
    if clustersAndTotalCounts is None:
        clustersAndTotalCounts = graphsPerCluster(clusterIDsList)
    else:
        for i in range(len(clusterIDsList)):
            
            if isSpanishGraph(clusterIDsList[i]):
                esPerClusterCounter[clustreIDsList[i]] += 1
                
                 
    
def listToCSV(targetFilePath, matrix, headerString):
    """
    write given matrix as csv file with given header
    """   
    targetFile = open(targetFilePath, "w")
    targetFile.write(headerString + "\n")   
    targetFile.write("\n".join([",".join([str(value) for value in line]) for line in matrix]))
    targetFile.close()
     
    
if __name__ == '__main__':
    import sys
    import codecs
    
    labels = []
    try:
        labelsFile = sys.argv[1]
        labels = codecs.open(labelsFile, "r").readlines()
    except:
        print "ERROR in cluster-stats.main: bad argument. Exiting..."
        return
    
    
    graphIDs, clusterIDs, predIDs, mentions = processLines(labels)
    
    #  count graphs per cluster
    clustersAndCounts = graphsPerCluster(clusterIDs)
    listToCSV(labelsFile + ".counts", clustersAndCounts, "cluster_ID,graphs_count")
    
    # count en-es mixture in each cluster
    clustersAndEsPercents = spanishPerCluster(clusterIDs, graphIDs) 
    listToCSV(labelsFile + ".esPercent", clustersAndEsPercents, "cluster_ID,%_spanish_predicates")
    
    # plot histograms
    
    
    
    
        
"""
import numpy as np
import matplotlib.mlab as mlab
import matplotlib.pyplot as plt

n, bins, patches = plt.hist(x, num_bins, normed=1, facecolor='green', alpha=0.5)
# add a 'best fit' line
y = mlab.normpdf(bins, mu, sigma)
plt.plot(bins, y, 'r--')
plt.xlabel('Smarts')
plt.ylabel('Probability')
plt.title(r'Histogram of IQ: $\mu=100$, $\sigma=15$')

# Tweak spacing to prevent clipping of ylabel
plt.subplots_adjust(left=0.15)
plt.show()
"""   
    
