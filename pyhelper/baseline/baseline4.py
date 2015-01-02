'''
Created on Dec 14, 2014

@author: Artem
'''
 
from xml.etree import ElementTree as ET
import os
import nltk
from progress.bar import Bar
from pyxdameraulevenshtein import normalized_damerau_levenshtein_distance as normalized_string_similarity # alternative: damerau_levenshtein_distance
from xml.etree import ElementTree as ET 
#from util.sendQuery import send_query

def get_sentence_from_srl(graphID, srlDirPath):
    """
    given the path to a graph file, 
    extract the corresponding article ID, determine language, 
    read the corresponding srl xml file, 
    get the correct sentence string from the xml file
    Input: graph id, e.g. en-1234452-1-12.graph
    Input: path of dir with all srl xml files
    Output: Sentence string or None if no srl file found
    """
    
    # decompose graph id
    parts = graphID.split("-")
    sentenceID = parts[2]
    srlFileName = parts[0] +"-" + parts[1] + "_srl.xml"
    srlFilePath = srlDirPath + srlFileName
    
    root = ET.parse(srlFilePath).getroot()
    sentenceString = root.find("sentences/sentence[@id='" + sentenceID +"']/text")
    
    if sentenceString is None:
        return None
    else:
        return sentenceString.text
#___ get_sentence() 
    
    
def send_query(sparqlQueryString, listOfVarNames):
    from SPARQLWrapper import SPARQLWrapper, JSON
    """
    Query DBpedia with a simple SELECT DISTINCT ?x?y ... WHERE {} 
    Input: sparql query string
    Return: list of result tuples [(xValue, yValue), (), ...]  
    """
    #sparql = SPARQLWrapper("http://dbpedia.org/sparql")
    sparql = SPARQLWrapper("http://aifb-ls3-merope.aifb.kit.edu:8890/sparql")
    sparql.setQuery(sparqlQueryString)
    sparql.setReturnFormat(JSON)
    answer = sparql.query().convert()
    
    result = []
    
    for item in answer["results"]["bindings"]:
        tempTuple = ()
        for varName in listOfVarNames:
            tempTuple = tempTuple + ( item[varName]["value"] , )
        
        result.append(tempTuple)
        
    return result

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
    
    
    
def filter_props(unfilteredPropsList):
    """
    Limit candidate props to to relevant namespaces:
    http://dbpedia.org/ontology/ or
    http://dbpedia.org/property/ or
    http://www.w3.org/1999/02/22-rdf-syntax-ns#type
    """
    okList = ["http://dbpedia.org/ontology/", 
              "http://dbpedia.org/property/", 
              "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"]
    filteredPropsList = []
    
    for prop in unfilteredPropsList:
        for namespaceOK in okList:
            if prop.startswith(namespaceOK):
                filteredPropsList.append(prop)
                break
    return filteredPropsList
#___ filter_props()



def get_candidate_props(entitiesOfQuestionSet):
    """
    for each entity: 
        query associated properties: 
        SELECT DISTINCT ?p WHERE {E ?p ?o} & {?s ?p E}
        Input: set of entity URIs that were recognised in the question
        Return: list of unique properties list(set()) associated with the entity set 
    """
    
    queryTemplateSubj = "select distinct ?prop where {0} {1} ?prop ?o .{2}"
    queryTemplateObj = "select distinct ?prop where {0} ?s ?prop {1} . {2}"
    
    resultPropsList = []
    
    for entURI in entitiesOfQuestionSet:
        # query DBpedia for candidate props
        qSubj = queryTemplateSubj.format("{", "<"+entURI+">", "}")
        qObj = queryTemplateObj.format("{", "<"+entURI+">", "}")
                 
        for tupl in send_query(qSubj, ["prop"]):
            resultPropsList.append(tupl[0])
        for tupl in send_query(qObj, ["prop"]):
            resultPropsList.append(tupl[0])
    return list(set(resultPropsList))
#___ get_candidate-props()
   
     
   
def get_question_string(questionID, lang):
    """
    Input: qald-4 question id and language as strings
    Return: question string
    """
    baseDir = "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/BaselineX/qald4-graphs/"
    subDir = "en-" + questionID + "_es-" + questionID + "/"
    srlFilePath = baseDir + subDir + lang + "-" + questionID + "-srl.xml"
    
    locationInXML = './/text'
    
    return ET.parse(srlFilePath).getroot().find(locationInXML).text
#___ get_question_string()



def tokenize_question(qString):
    # remove the last token. it is punctuation.
    return nltk.word_tokenize(qString)[0:-1]



def get_gold_props(questionID):
    """
    Gold-standard qald-4 props were prepared in files.
    Return: list of gold props for each question 
    """
    baseDir = "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/BaselineX/qald4-graphs/" 
    subDir = "en-" + questionID + "_es-" + questionID + "/"
    goldPropsFilePath = baseDir + subDir + questionID + "-gold-props"
    
    return [line.strip() for line in open(goldPropsFilePath, "r").readlines()]
#___ get_got_props()    



def get_prop_label(propertyURI):
    """
    Take the substring after the last "/".
    """
    return propertyURI.strip().split("/")[-1].lower() 



def prune_candidate_props(questionTokensList, candidatePropsList, MIN_SIMILARITY):
    """
    Add property to list if at least one question token has
        sim(token,prop)>=MIN_SIMILARITY
        
    Input: tokenized question (list of strings)
    Input: list of candidate property URIs
    Input: minimum similarity threshold for filtering out props
    Return: pruned list of property URIs
    """
    tokens = [token.lower() for token in questionTokensList]
    prunedPropsList = []
    
    # 
    for i in range(len(candidatePropsList)):
        
        
        
        for j in range(len(tokens)):
            sim = 1.0 - normalized_string_similarity(tokens[j], get_prop_label(candidatePropsList[i]))     
            if sim >= MIN_SIMILARITY:
                #print sim, " ", tokens[j], get_prop_label(candidatePropsList[i]) 
                prunedPropsList.append(candidatePropsList[i])
                break
            
    #return list(set(prunedPropsList))
    return list(set(candidatePropsList))
#___ reduce_candidate_props()


import sys
if __name__ == '__main__':
    """
    for each question: for each entity: 
        query associated properties: SELECT DISTINCT ?p WHERE {E ?p ?o} & {?s ?p E}  
    prune those properties most dissimilar with quesiton tokens
    
    Annotations are already provided in csv files for each question.
    """

    L = ["en"] #
    MIN_SIMILARITY = [0.4, 0.5, 0.6, 0.7, 0.8, 0.9] [int(sys.argv[1])] 
    
    baseDir = "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/BaselineX/qald4-graphs/"
     
    progress = Bar("fertig", max=len(os.listdir(baseDir)))
    
    #resultFileEN = open("en-baseline-result", "w")
    #resultFileES = open("es-baseline-result", "w")
    #resultFilesDict = {"en": resultFileEN, "es": resultFileES}
    resultFilesDict = {str(L[0]) : open(L[0]
                                        +"-GRAPH-ENTS_ALL-PROPS_" 
                                        + str(MIN_SIMILARITY) 
                                        + "_SIMTHRESH-baseline-result", "w")}
    print str(MIN_SIMILARITY)
    for qdir in os.listdir(baseDir):
        qid = qdir.split("-")[-1]
        #if "21" not in qid: continue
        #print qid
        for lang in L:
            
            
            #===================================================================
            # # 1. get entities
            # # ALL ENTITIES FROM QUESTION
            # annotFileName = "".join([baseDir, qdir, "/", lang, "-", qid, "-annot.extracted"])
            # entsMetadata = [line.strip() for line in open(annotFileName, "r").readlines()]
            # entsList = [line.split(",")[0] for line in entsMetadata]
            #===================================================================
            
            #===================================================================
            # ONLY ENTITIES FROM GRAPHS
            #===================================================================

            annotFileName = "".join([baseDir, qdir, "/", lang, "-graph-annots"])
            try: # if no anntos in the graphs for that question available
                entsMetadata = [line.strip() for line in open(annotFileName, "r").readlines()]
            except: 
                continue
            
            entsList = [line.split(",")[0] for line in entsMetadata]
            
            # abort if no entities found !!!
            if len(entsList) == 0: 
                
                continue
            
            # 2. get properties for those entities
            candProps = filter_props(get_candidate_props(entsList))
            
            # 3. tokenize
            questionString = get_question_string(qid, lang)
            tokens = tokenize_question(questionString)
            
            # 4. prune candidate properties
            prunedProps = prune_candidate_props(tokens, candProps, MIN_SIMILARITY)
            
            # 5. get gold standard properties from qald4
            goldstdProps = get_gold_props(qid)
            
            # 6. score the results and save file
            intersectionSize = len(set(prunedProps).intersection(set(goldstdProps)))
            P, R, F1 = update_scores(intersectionSize, len(goldstdProps), len(prunedProps))
            
            # 7. update result file
            tempFile = resultFilesDict.get(lang)
            hitIndicator = (intersectionSize>0)*"+" + "Q: "
            
            content = "\n\t".join([hitIndicator + questionString, "_____ANNOTATIONS_____"]
                                  + [""+ line.decode("utf-8").split(",")[0] #[1] 
                                     #+ ")\t" 
                                     #+ line.decode("utf-8").split(",")[0] 
                                     #+ "\tw=" +line.decode("utf-8").split(",")[2] 
                                     for line in entsMetadata]
                                  + ["_____GOLD-STD PROPERTIES_____"] 
                                  + goldstdProps 
                                  + ["_____CANDIDATE_____"]
                                  + ["C " + prop for prop in prunedProps]
                                  + ["-----------"] 
                                  + ["P=" + str(P) + "    R=" + str(R) + "    F1=" + str(F1)]
                                  )
            
            tempFile.write(content.encode("utf-8") + "\n\n")
            
            
        #___ loop over languages
        
        progress.next()  # next question
        
        #time.sleep(0.5)
        
        
        
    #___ loop over questions
    resultFilesDict.get(lang).write("\ntotal TP \t " + str(__TP))
    resultFilesDict.get(lang).write("\ntotal FP: \t " + str(__FP))
    resultFilesDict.get(lang).write("\ntotal FN: \t " + str(__FN))
    
    if not(__TP==0 or __FP==0 or __FN==0):
        PRECISIONglobal = 1.0 * __TP/(__TP + __FP)
        RECALLglobal = 1.0 * __TP/(__TP + __FN)
    else:
        PRECISIONglobal = 0
        RECALLglobal = 0
        
    resultFilesDict.get(lang).write("\ntotal PRECISION \t " + str(PRECISIONglobal))
    resultFilesDict.get(lang).write("\ntotal RECALL \t " + str(RECALLglobal))
    resultFilesDict.get(lang).write("\ntotal F1 \t " + str( 2.0 * PRECISIONglobal * RECALLglobal / ( PRECISIONglobal + RECALLglobal) ))
    
    progress.finish()
    
    for resFile in resultFilesDict.values():
        resFile.close()
    


