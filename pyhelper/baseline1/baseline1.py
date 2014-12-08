#!/usr/bin/python
'''
Created on Dec 5, 2014
@author: Artem
'''




import fyzz                         # SPARQL query parser
import sys
import codecs
import nltk                         # natural language toolkit
import xml.etree.ElementTree as ET
from progress.bar import Bar
from collections import Counter
from pyxdameraulevenshtein import normalized_damerau_levenshtein_distance as normalized_string_similarity # alternative: damerau_levenshtein_distance


def get_props(sparqlQuery):
    """
    Input: SPARQL query String.
    Output: list of used properties in the WHERE clause or empty list if none found
    """
    
    resultProps = []
    
    try:
        # WHERE clause consists of triples
        whereStatement = fyzz.parse(sparqlQuery).where
        
        # property is the second item in the tuple
        # it consists of a namespace prefix and the actual property
        
        for trip in whereStatement:
            propURI = trip[1][0] + trip[1][1]
            resultProps.append(propURI)        
        
    except:
        sys.stderr.write(sparqlQuery + "\n")
        pass 
        #print("get_props() threw Exception")
    return resultProps
    


def print_all_qald_props(qaldTrainXMLPpath="/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/QALD_challenge/qald-4-data/qald-4_multilingual_train_withanswers.xml"):
    """
    Prints out all property URIs from the QALD-4 training set. 
    Call in bash: print_all_props > all-qald-props.txt
    Input: file path of the qald training xml.
    """
    qaldTrainingFilePath = qaldTrainXMLPpath
    qaldRawFile = codecs.open(qaldTrainingFilePath, "r")
    
    # find all <query> elements in the XML
    queries = ET.parse(qaldRawFile).getroot().findall(".//question/query")
    
    i=0
    propsSet        = Counter()     # count how often each property URI occurs in the qald training set   
    namespaceSet    = Counter()     # count occurences of namespaces
    
    for q in queries:
        qtext = q.text
        try:
            # WHERE clause consists of triples
            whereStatement = fyzz.parse(qtext).where
            
            # property is the second item in the tuple
            # it consists of a namespace prefix and the actual property           
            for trip in whereStatement:
                propURI = trip[1][0] + trip[1][1]
                propsSet[propURI]       += 1
                namespaceSet[trip[1][0]] += 1
                print propURI
                
            
        except:
            if "WHERE" not in qtext:
                i+=1
    
    #===========================================================================
    # for item in sorted(set(propsSet.items())):
    #     # print property URI and its counts
    #     print item[0], "\t", item[1]
    #  
    # print namespaceSet
    #===========================================================================

#===========================================================================

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
#===========================================================================


def update_scores(numHits, numGoldStandard, numCandidates):
    """
    Update the classification measures after each question.
    numHits = number of hits
    numGoldStandard = num of gold-standard props
    numCandidates = num of candidate props  
    return: [precision, recall, f1] for each call
    """
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
    
    
    
def baseline1():  
    """
    For each token in English QALD questiion: 
        -extract properties from the target SPARQL query
        -find DBpedia property with most similar label.
        -identify and count matching properties
    """
    
    #===========================================================================
    dbpediaPropsFile = "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/Baseline-1/dbpedia-properties-combined"
    qaldTrainingXML  = "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/QALD_challenge/qald-4-data/qald-4_multilingual_train_withanswers.xml"
    #===========================================================================
    
    # read dbpedia properties and get the last bit as labels
    dbpPropsURIs = [uri.strip() for uri in open(dbpediaPropsFile, "r").readlines()]
    dbpPropsLabels = [propURI.strip().split("/")[-1].lower() for propURI in dbpPropsURIs]
        
    # read qald questions from xml file
    questionItems = ET.parse(open(qaldTrainingXML, "r")).getroot().findall(".//question")
    
    progressbar = Bar('Processing', max=len(questionItems))

    for question in questionItems:
        candidatePropsSet = set()
        questionString = question.find('string[@lang="en"]').text.lower()
        print questionString
        
        # get and print the target properties
        targetQueryString = question.find("query").text
        targetPropsSet = set(get_props(targetQueryString))
        if len(targetPropsSet) == 0:
            print "\t", "exception: failed to parse target query "#, targetQueryString
            progressbar.next()
            continue
        print "\t", "gold-standard properties: "
        for targetPropURI in targetPropsSet: print "\t", ">", "\t", targetPropURI
        
        
        
        tokens = nltk.word_tokenize(questionString)[0:-1]   # omit last item, i.e. the end-of-sentence punctuation
        for i in range(len(tokens)):              
            winnerDBpPropURI    = None
            winnerSimilarity    = 0.0     
            tempSim             = 0.0
            
            
            for j in range(len(dbpPropsLabels)):
                tempSim = 1.0 - normalized_string_similarity(tokens[i], dbpPropsLabels[j])        # compute string similarity
                if tempSim > winnerSimilarity:
                    winnerDBpPropURI    = dbpPropsURIs[j]
                    winnerSimilarity    = tempSim
            #___[iterate over all dbp props for each token]
            
            if winnerDBpPropURI is not None:
                candidatePropsSet.add(winnerDBpPropURI)
            print "\tCANDIDATE: ", tokens[i] , "\t", winnerDBpPropURI, "\t", winnerSimilarity
            
        #___[iterate over all tokens in the question]
        
        hits = targetPropsSet.intersection(candidatePropsSet)   # for each question
       
        #====================== update scores
        p, r, f1 = update_scores(len(hits), len(targetPropsSet), len(candidatePropsSet))
        if(len(hits)>0):
            print "\t", "+ tp: ", str(len(hits)), " precision: ", str(p), " recall: ", str(r), " f1: ", f1
        else:
            print "\t", "tp: ", str(len(hits)), " precision: ", str(p), " recall: ", str(r), " f1: ", f1
        #======================
        
        progressbar.next()
        print "\n"
    #___[iterate over all questions]
    
    progressbar.finish()
    
    print "total TP \t ", str(__TP)
    print "total FP: \t ", str(__FP)
    print "total FN: \t ", str(__FN)
    PRECISIONglobal = 1.0 * __TP/(__TP + __FP)
    RECALLglobal = 1.0 * __TP/(__TP + __FN)
    print "total PRECISION \t ", str(PRECISIONglobal)
    print "total RECALL \t ", str(RECALLglobal)
    print "total F1 \t ", str( 2.0 * PRECISIONglobal * RECALLglobal / ( PRECISIONglobal + RECALLglobal) )
#___[def baseline1()]               

    

        
                
            
            
            
            
        







if __name__ == '__main__':

    functionName = sys.argv[1]
     
    if functionName     == "print-all":
        qaldTrainFileXML = sys.argv[2]
        print_all_qald_props(qaldTrainFileXML) 
        
    elif functionName   == "baseline":
        baseline1()
        
    else:
        print "unknown first argument"
        
        
        