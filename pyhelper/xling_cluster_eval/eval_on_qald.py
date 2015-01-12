"""
To run evaluation, first perform these steps:
1.
clusteringeval.clusterID_to_predicate.main
    to create preprocessed (augmented) clustering result file

2. (optional)
cluster2props.props_to_clusters.main
    to test the scoring of properties in each cluster 
    
3. (optional)
clusteringeval.cluster_stats.main
    to create histograms and human-readable cluster summaries
"""
'''
Created on Jan 4, 2015

@author: pilatus
'''
import sys
import os
import codecs
from baseline import baseline4
from cluster2props import props_to_clusters
from util.config import GET_CONF_DICT
import operator


#===========================================================================  
# classification measures declaration
__TP = 0        # number of hits
#__TN = 0       # not important
__FP = 0        # num of candidate props - num of hits
__FN = 0        # num of gold-standard props - num of hits 
#__PRECISION = 0 # TP / (TP + FP)
#__RECALL = 0    # TP / (TP + FN)
#__F1 = 0        # 2*P*R/(P+R)
#===========================================================================

def reset_scores():
    """
    Set global score variables back to 0.
    """
    global __TP       
    global __FP    
    global __FN
    __TP = 0
    __FP = 0
    __FN = 0
#___ reset_scores()

  
def update_scores(numHits, numGoldStandard, numCandidates):
    """
    Update the classification measures after each question.
    numHits = number of hits
    numGoldStandard = num of gold-standard props
    numCandidates = num of candidate props  
    return: [precision, recall, f1] for each call
    """
    
    if numGoldStandard == 0 or numCandidates == 0:
        return ["-", "-", "-"]
    else:
        global __TP       
        global __FP    
        global __FN        
    
        __TP += numHits
        __FP += (numCandidates - numHits)
        __FN += (numGoldStandard - numHits)
        
        # return per-question scores
        p = 1.0 * numHits / numCandidates
        r = 1.0 * numHits / numGoldStandard
        if (p+r) > 0.0 :
            f1 = 2.0 * p *r / (p + r)
        else:
            f1 = 0.0
        return [p, r, f1]
#___ update_scores()    
    
    
def get_graphID_clusterID_dict(preprClusteringLabelsList):
    """
    Create dict of graphID to clusterID. 
    Graph labels have no file name ending.
    Input: preprocessed list of graph-cluster labels 
    Output: dict with { graphID : clusterID } 
    """
    
    graphClusterDict = dict()
    
    for line in preprClusteringLabelsList:
        # en-106-1-1.json,4.0000000e+00,generate.01,Give
        parts = line.split(",")
        graphID = parts[0].split(".")[0] # erase ending
        clusterID = parts[1]
        graphClusterDict.update( { graphID : clusterID } )
        
    return graphClusterDict
#___ get_graphID_cluster_dict()



def calculate_props_ranking(preprocessedClusteringResultFileName, propsHomedirPath, entsHomedirPath):
    """
    Specify a properties ranking method here (tfidf, cluster-relative, cluster-absolute).
    Output: dict of { clusterID : dict {propURI : ranking score} }
    """
    
    # e.g. 
    # preprocessedClusteringResultFileName = \
    #    "/home/pilatus/WORK/pred-clust/data/clustering-longarticles-qald-25k/ClusteringResultsLabeled/SCResultmsm12-sparse.csvdimensions100Cluster1000Normalization1.txt"
    # propsHome ="/home/pilatus/WORK/pred-clust/data/props-longarticles-25k/"
    # entsHome = "/home/pilatus/WORK/pred-clust/data/entities-longarticles-25k/"
    
    clustersEntsDict = props_to_clusters.getClusterGraphsDict( open(preprocessedClusteringResultFileName, "r").readlines() )
    
    clustersPropsDict = props_to_clusters.get_clusters_props_dict( clustersEntsDict, entsHomedirPath, propsHomedirPath )
    #print "clusters ents dict ", str(len(clustersEntsDict))
    #print "clusters props dict ", str(len(clustersPropsDict)) 
    
    # cluster-relative frequency ranking:
    #return props_to_clusters.produce_cluster_relative_frequency_ranking( clustersPropsDict )
    # tfidf ranking:
    #return props_to_clusters.produce_cluster_tfidf_ranking( clustersPropsDict )
    # cluster-absolut frequency ranking:
    return props_to_clusters.produce_cluster_absolute_frequency_ranking(clustersPropsDict)
    
#___ calculate_props_ranking()




if __name__ == '__main__':
    """
    Cross-lingual clustering evaluated on QALD4 properties detection task.
    given: a pre-processed clustering.
    calculate props-ranking dict.
    for each question ID 1-200: 
        get all graphs of question
        for each graph:
            determine clusterID
            get props of that cluster
            (rerank props based on similarity to question token)
            calculate P, R, F1 
    call it like 
    export i=0;
    for file in `/bin/ls /home/pilatus/WORK/pred-clust/data/clustering-longarticles-qald-25k/ClusteringResultsLabeled/*.txt`; do 
    python $W/predclust/pyhelper/xling_cluster_eval/eval_on_qald.py $file en $i; 
    export i=$((i+1)); 
    done
    """
    
    #===========================================================================
    # read some commandline arguments
    #===========================================================================
    clusterLabels = []
    lang = None
    try:
        labelsFile = sys.argv[1]
        #labelsFile = "/home/pilatus/WORK/pred-clust/data/clustering-longarticles-qald-25k/ClusteringResultsLabeled/SCResultINDmSum0.3-sparse.csvdimensions50Cluster500Normalization3.txt"
        clusterLabels = [line.strip() for line in codecs.open(labelsFile, "r").readlines()]
    except:
        print "ERROR in eval_on_qald.main: bad clustering labels file:"
        print "Exiting..."
        exit
        
    try:
        lang = sys.argv[2].strip()
        #lang = "es"
    except: 
        print "ERROR in eval_on_qald.main: bad language argument. Exiting..."
        exit
        
    resultFileName = "/home/pilatus/WORK/pred-clust/data/clustering-shorts-qald-25k/EVAL/eval_"  + lang + "_v2.csv" 
    if os.path.exists(resultFileName):
        resultLog = open(resultFileName, "a")
    else:
        resultLog = open(resultFileName, "a")
        resultLog.write("clustering configuration\t precision \t recall \t f1 \n")
    
        
    #===========================================================================
    # read some configs
    #===========================================================================
    conf = GET_CONF_DICT()
    qIDsDict            = conf.get("qald-graph.ids.dict")   # read list of all question IDs
    #===========================================================================
    # propsHomedirPath    = conf.get("25k-long.props")        
    # entsHomedirPath     = conf.get("25k-long.ents")
    #===========================================================================
    propsHomedirPath    = conf.get("25k-short.props")        
    entsHomedirPath     = conf.get("25k-short.ents")
    
    #===========================================================================
    # prepare some infrastructure
    #===========================================================================
    # create dict of graphIDs and clusterIDs
    graphsClustersDict = get_graphID_clusterID_dict( clusterLabels )  
    # get ranked props of cluster like that:
    # { clusterID : dict{ propURI : tfidf ranking inside cluster } }
    rankedPropsDict = calculate_props_ranking(labelsFile, propsHomedirPath, entsHomedirPath)
    
    
    
    #===========================================================================
    # main eval loop
    #===========================================================================
    for qID in range(1,201):
        
        qID = str(qID)
        # get all graphs of question
        graphsInQuestion = qIDsDict.get( lang + "-" + qID )
        
        #=======================================================================
        # skip to next question if this one has no graphs 
        #=======================================================================
        if graphsInQuestion is None: 
            continue
        
        
        
        # get question string and tokenize it
        qTokensList = baseline4.tokenize_question ( baseline4.get_question_string( qID, lang ) )
        
        # get gold props of question
        qGoldPropsSet = set( baseline4.get_gold_props(qID) )
        numGoldProps = len( qGoldPropsSet )
        
        #print "num clusters ", str(len(set(graphsClustersDict.values())))
        # for each question, get a list of its graphs' clusterIDs
        clustersOfQuestion = []
        for graphID in graphsInQuestion:
            clusterID = graphsClustersDict.get( graphID )
            clustersOfQuestion.append( clusterID )
        #___ make list of clsterIDs for that question


        # for each cluster of that question:
        #    get rank-sorted list of props of that cluster
        #    add the <numGoldProps> best props to candidate set
        # result is a list of tuples ( propURI , rank )
        candidateProps = []
        for clusterID in clustersOfQuestion:
            propsOfClusterDict = rankedPropsDict.get( clusterID )
            # if cluster has no props associated with it, it is not in the dict. Skip it.
            if not propsOfClusterDict:
                continue
            else:
                candidateProps += sorted( propsOfClusterDict.items(), key=operator.itemgetter(1), reverse=True) [ 0 : numGoldProps ]
        
        # if there are more candidates than <numGoldProps>
        # resort and prune the list of tuples to max <numGoldProps> items
        finalCandidatePropsSet = set()
        if len(candidateProps) > numGoldProps:
            finalCandidatePropsTuples = sorted( candidateProps, key=operator.itemgetter(1), reverse=True) [0 : numGoldProps]
            finalCandidatePropsSet = set( [tupl[0] for tupl in finalCandidatePropsTuples] )
        else:
            finalCandidatePropsSet = set( [tupl[0] for tupl in candidateProps] )
 
        # update scores
        hits = finalCandidatePropsSet.intersection( qGoldPropsSet )
        numHits = len( hits ) 
        p, r, f1 = update_scores(numHits , numGoldProps, len(finalCandidatePropsSet))
        
        
        # print winner props to console
        if numHits >0:
            #print "question: ",  qID
            #print "\t", "clusters:\t", clustersOfQuestion  
            #print "\t", candidateProps
            #print "\t", finalCandidatePropsSet.intersection( qGoldPropsSet )
            #print [p,r,f1]
            for hitProp in hits:
                print labelsFile.split("/")[-1].replace(".txt", ""), "\t", hitProp
        
        
    #___ loop over all qald questions    

    if not(__TP==0 or __FP==0 or __FN==0):
        PRECISIONglobal = 1.0 * __TP/(__TP + __FP)
        RECALLglobal = 1.0 * __TP/(__TP + __FN)
        F1global = 2.0 * PRECISIONglobal * RECALLglobal / ( PRECISIONglobal + RECALLglobal)
    else:
        PRECISIONglobal = 0
        RECALLglobal = 0
        F1global = 0
        
    #print "hits: ", __TP
    resultLog.write( labelsFile.split("/")[-1].replace(".txt", "") + "\t" 
                     + str(PRECISIONglobal) + "\t" 
                     + str(RECALLglobal) + "\t" +str(F1global) + "\n")
  
    resultLog.close()
    print sys.argv[3]   