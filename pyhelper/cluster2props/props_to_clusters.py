'''
Created on Dec 21, 2014

@author: pilatus
'''

from clusteringeval.cluster_stats import getClusterGraphsDict
from collections import Counter
import operator

def get_prop_exclude_set():
    """
    Exclude structural wiki props.
    Output: set of properties to exclude. 
    """
    #===============================================================================
    # count   propURI (25k-noqald-long)
    # 1345    http://dbpedia.org/ontology/wikiPageExternalLink
    # 1418    http://dbpedia.org/ontology/wikiPageDisambiguates
    # 1434    http://dbpedia.org/ontology/thumbnail
    # 1730    http://dbpedia.org/ontology/abstract
    # 1733    http://dbpedia.org/ontology/wikiPageRedirects
    # 1770    http://dbpedia.org/ontology/wikiPageRevisionID
    # 1770    http://dbpedia.org/ontology/wikiPageID
    # 1770    http://dbpedia.org/ontology/wikiPageWikiLink
    #===============================================================================
    propsToExcludeSet = set()
    
    excludeList = ["http://dbpedia.org/ontology/wikiPageExternalLink",
                   "http://dbpedia.org/ontology/wikiPageDisambiguates",
                   "http://dbpedia.org/ontology/thumbnail",
                   "http://dbpedia.org/ontology/abstract",
                   "http://dbpedia.org/ontology/wikiPageRedirects",
                   "http://dbpedia.org/ontology/wikiPageRevisionID",
                   "http://dbpedia.org/ontology/wikiPageID",
                   "http://dbpedia.org/ontology/wikiPageWikiLink"]
    
    for badProp in excludeList:
        propsToExcludeSet.add(badProp)
        
    return propsToExcludeSet
    
    
def get_all_props_of_cluster(entURIsList, propsBaseDirPath):
    """
    For all the graphIDs provided, collect the corresponding properties.
    graphID -> graphID.entities -> entity labels -> properties
    In the next step, this list of cluster props needs to be ranked.
    Input: complete list of entities that occur in one cluster
    Output: list of property URIs that correspond to all the entities from that cluster 
    
    !!! Note: 
    1. Some ent URIs contain escape char "\". It is removed in this method
    2. Certain props are excluded via a stop-list.
    !!!
    """
    
    resultPropsList = []
    propsToExcludeSet = get_prop_exclude_set()
        
    # 1. split off the entity label = fileName of the properties file
    # 2. for each uri, read its props, add props to return list
    for entURI in entURIsList:
        
        # !!! remove escape char 
        entLabel = entURI.replace("\\", "").split("/")[-1]
        propsFileName = propsBaseDirPath + entLabel
        
        props = []
        try:
            props = [prop.strip() for prop in open(propsFileName, "r").readlines()]
        except:
            #print "get_all_props_of_cluster(): props file not found: ", propsFileName 
            continue
            
        # lines of entity file example: <uri> \t <weight>
        # exclude props via stop-list
        resultPropsList = resultPropsList + [prop for prop in props if prop not in propsToExcludeSet]
    
    return resultPropsList
        
        
def get_clusters_props_dict(clustersEntsDict, entsHomeDir, propsHomeDir):
    """
    Given a dict of clusterID - [list of entURIs] of that cluster, 
    create a dict of clusterID - [list of props] of that cluster.
    Input: dict {clusterID string : list of ent URIs}
    Input: home directories of .entities and props files
    Output: dict of {clusterID string : list of prop URIs}
    If a cluster has no props, then it is not added to the dict.
    """
    resultClustersPropsDict = dict()
    
    for clusterID in sorted(clustersEntsDict.keys()):
        
        # get ents from graphIDs
        entURIsOfCluster = get_all_entities_of_cluster( clustersEntsDict.get(clusterID), entsHome )
        
        # get props from ents
        propURIsOfCluster = get_all_props_of_cluster( entURIsOfCluster, propsHome )   
    
        if not propURIsOfCluster:
            continue
        else:
            resultClustersPropsDict.update( { clusterID : propURIsOfCluster } )
    #___ iterate over all cluster IDs

    return resultClustersPropsDict



def get_all_entities_of_cluster(graphIDsInClusterList, entsBaseDirPath):
    """
    For all the graphIDs provided, collect the corresponding entities.
    graphID -> graphID.entities 
    Input: list of graphIDs in one cluster, graphID must be without file ending
    Output: entity URIs from all graphs of that cluster 
    """
    resultEntsList = []
    
    # 1. read .entities file for each graph ID
    # 2. strip the URIs and add them to the result list
    for graphID in graphIDsInClusterList:
        
        entsFile = None
        entsFileName = entsBaseDirPath + graphID + ".entities"
        try:
            entsFile = open(entsFileName, "r")
        except: 
            #print "get_all_entities_of_cluster(): ents file not found: ", entsFileName
            continue
        ents = entsFile.readlines()
        
        resultEntsList = resultEntsList + [ ent.split("\t")[0].strip() for ent in ents ]
    #___ for each graphID in that cluster
      
    return resultEntsList


def produce_global_frequenc_ranking(clustersPropsDict):
    """
    Rank all props in dataset by their frequency.
    Input: dict of { clusterID : [props list of cluster] }
    Output: dict of  {propURI : global freq-ranking } 
    Sort it later by freq value in descending order.
    """
    
    globalPropsRankingDict = Counter()
    
    for clusterID in clustersPropsDict:
        for prop in clustersPropsDict.get(clusterID):
            globalPropsRankingDict[prop] += 1
    #___
    
    return globalPropsRankingDict
#___ freq-rank properties in enire dataset
    
   
    
def produce_cluster_frequency_ranking(clustersPropsDict):
    """
    Creates a cluster-specific frequency ranking of properties.  
    Input: dict of { clusterID : [props list of cluster] }
    Output: dict of { clusterID : dict {propURI : ranking inside cluster } }
    Usage: clusterID -> propURI -> freq-ranking
    """
    
    clustersDictsDict = dict()
    
    for clusterID in clustersPropsDict:
        propsInClusterRanking = Counter()
        
        for prop in clustersPropsDict.get(clusterID):
            propsInClusterRanking[ prop ] += 1
            
        clustersDictsDict.update( { clusterID : propsInClusterRanking } )
        
    
    
    return clustersDictsDict
#___ create dict of freq-ranking per cluster

 
#def produce_cluster_relative_ranking(clustersPropsDict):
    """
    Creates a cluster-specific tfidf-like ranking of properties.  
    Input: dict of { clusterID : [props list of cluster] }
    Output: dict of { clusterID : dict {propURI : tfidf ranking inside cluster } }
    Usage: clusterID -> propURI -> tfidf-ranking
    """
    # TODO
def produce_cluster_relative_ranking(clustersPropsDict):
    """
    Creates a cluster-specific tfidf-like ranking of properties.  
    Input: dict of { clusterID : [props list of cluster] }
    Output: dict of { clusterID : dict {propURI : tfidf ranking inside cluster } }
    Usage: clusterID -> propURI -> tfidf-ranking
    """
    resultDictsDict = dict()
    
    byClusterFreqRankedPropsDict = produce_cluster_frequency_ranking(clustersPropsDict)
    globalFreqRankedPropsDict    = produce_global_frequenc_ranking(clustersPropsDict)
    numPropsInDataset           = len(globalFreqRankedPropsDict)
    
    for clusterID in byClusterFreqRankedPropsDict.keys():
        
        tempPropsRanksDict = dict()
        currentClusterDict = byClusterFreqRankedPropsDict.get(clusterID)
        clusterSize        = len (currentClusterDict) 
        # print clusterID
        for propURI in currentClusterDict.keys():
            # relativeInCluster / relativeGlobal
            localPropFreqRank = currentClusterDict.get( propURI )
            globalPropFreqRank= globalFreqRankedPropsDict.get(propURI)
            print str(localPropFreqRank), "\t", str(clusterSize), "\t", str( globalPropFreqRank), "\t", str(numPropsInDataset) 
            relativeRanking =  (1.0 *localPropFreqRank / clusterSize) / (1.0 * globalPropFreqRank / numPropsInDataset) 
            print "\t", str(relativeRanking)
            tempPropsRanksDict.update ( { propURI : relativeRanking } )
        #___ loop over all props in cluster
        
        resultDictsDict.update( { clusterID : tempPropsRanksDict } )
    #___ loop over all clusters
    
    return resultDictsDict
#___ produce_global_relative_ranking()
 
if __name__ == '__main__':
    """
    create dict from clustering -> get entities of cluster -> get properties of entities
    """
    preprocessedClusteringResultFileName = \
    "/home/pilatus/WORK/pred-clust/data/clustering-longarticles-25k/ClusteringResultsLabeled/mSum-sparse10eigsSpectralClustering25k2000ClusterNormalization2.csv"
    propsHome ="/home/pilatus/WORK/pred-clust/data/props-longarticles-25k/"
    entsHome = "/home/pilatus/WORK/pred-clust/data/entities-longarticles-25k/"
    
    clustersEntsDict = getClusterGraphsDict( open(preprocessedClusteringResultFileName, "r").readlines() )
    
    clustersPropsDict = get_clusters_props_dict( clustersEntsDict, entsHome, propsHome )
    
    sumprops = 0
    propslist = []
    print len(clustersPropsDict.values())
    #===========================================================================
    # for clID in clustersPropsDict.keys():
    #     print clID
    #     propslist += clustersPropsDict.get(clID)
    #     print "\t", str(len(clustersPropsDict.get(clID))), "\t", str(len(set(clustersPropsDict.get(clID))))
    #===========================================================================
        
    #print "-----------"
    #print str(len(propslist)), "\t", str(len(set(propslist)))
    #print "-----------"
    sortedPropsGlobal = sorted(produce_global_frequenc_ranking(clustersPropsDict).items(), key=operator.itemgetter(1), reverse=False)
    #print "\n".join([str(tupl[1]) +"\t"+ tupl[0] for tupl in sortedPropsGlobal])
    
    perClusterFreq = produce_cluster_frequency_ranking(clustersPropsDict)
    perClusterRelative = produce_cluster_relative_ranking(clustersPropsDict)
    
    print len(perClusterFreq)
    print len(perClusterRelative)
    
    #===========================================================================
    # for clID in perClusterRelative:
    #     sortedProps = sorted(perClusterRelative.get(clID).items(), key=operator.itemgetter(1), reverse=True)[0:3]
    #     print clID
    #     for p in sortedProps:
    #         print "\t", p[1], "\t", p[0]
    #===========================================================================
        
