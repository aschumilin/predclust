'''
Created on Jan 4, 2015

@author: pilatus
'''
import sys
import codecs
from baseline.baseline4 import get_gold_props
from baseline.baseline4 import get_question_string
from baseline.baseline4 import tokenize_question
from util.config import GET_CONF_DICT


#===========================================================================  
# classification measures declaration
__TP = 0        # number of hits
#__TN = 0       # not important
__FP = 0        # num of candidate props - num of hits
__FN = 0        # num of gold-standard props - num of hits 
__PRECISION = 0 # TP / (TP + FP)
__RECALL = 0    # TP / (TP + FN)
__F1 = 0        # 2*P*R/(P+R)
#===========================================================================


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
    """
    clusterLabels = []
    lang = None
    try:
        labelsFile = sys.argv[1]
        clusterLabels = [line.strip() for line in codecs.open(labelsFile, "r").readlines()]
        #print labelsFile
    except:
        print "ERROR in eval_on_qald.main: bad clustering labels file:"
        print labelsFile
        print "Exiting..."
        exit
        
    try:
        lang = sys.argv[2].strip()
    except: 
        print "ERROR in eval_on_qald.main: bad language argument. Exiting..."
        exit
        
    # read list of ald question IDs
    conf = GET_CONF_DICT()
    qIDsDict = conf.get("qald-graph.ids.dict")
    
    # create dict of graphIDs and clusterIDs
    graphsClustersDict = get_graphID_clusterID_dict(clusterLabels)
    
    
    for qID in range(0,201):
        
        # get all graphs of question
        graphsInQuestion = qIDsDict.get(lang + "-" + qID)
        print qID
        
        for graphID in graphsInQuestion:
            clusterID = graphsClustersDict.get(graphID)
            
    #___ loop over all qald questions    
        
    get_gold_props("12")
    qString = get_question_string("12", "en")
    tokenize_question(qString)   
    