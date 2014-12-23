'''
Created on Dec 21, 2014

@author: pilatus
'''

from clusteringeval.cluster_stats import getClusterGraphsDict
from collections import Counter

def get_all_props_of_cluster(entURIsList, propsBaseDirPath):
    """
    For all the graphIDs provided, collect the corresponding properties.
    graphID -> graphID.entities -> entity labels -> properties
    In the next step, this list of cluster props needs to be ranked.
    Input: complete list of entities that occur in one cluster
    Output: list of property URIs that correspond to all the entities from that cluster 
    
    !!! 
    Some ent URIs contain escape char "\". It is removed in this method
    !!!
    """
    
    resultPropsList = []
    
    # 1. split off the entity label = fileName of the properties file
    # 2. for each uri, read its props, add props to return list
    for entURI in entURIsList:
        
        entLabel = entURI.replace("\\", "").split("/")[-1]
        propsFileName = propsBaseDirPath + entLabel
        
        props = []
        try:
            props = open(propsFileName, "r").readlines()
        except:
            #print "get_all_props_of_cluster(): props file not found: ", propsFileName 
            continue
            
        # lines of entity file example: <uri> \t <weight>
        resultPropsList = resultPropsList + [prop.strip() for prop in props]
    
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
            print "get_all_entities_of_cluster(): ents file not found: ", entsFileName
            continue
        ents = entsFile.readlines()
        
        resultEntsList = resultEntsList + [ent.split("\t")[0].strip() for ent in ents]
    #___ for each graphID in that cluster
      
    return resultEntsList


def rank_by_frequency(propsList):
    """
    Rank props of a cluster by their frequency
    Input: list of prop URIs of a cluster
    Output: dict ot { propURI : ranking }
    """
    propCounts = Counter()
    
    for prop in propsList:
        propCounts[prop] += 1
    
    
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
    for clID in clustersPropsDict.keys():
        print clID
        propslist += clustersPropsDict.get(clID)
        print "\t", str(len(clustersPropsDict.get(clID))), "\t", str(len(set(clustersPropsDict.get(clID))))
        
    print "-----------"
    print str(len(propslist)), "\t", str(len(set(propslist)))
            
