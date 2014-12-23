'''
Created on Dec 17, 2014

@author: Artem
'''
import json

def get_graph_info(graphJsonDict):
    """
    Print predicate information in readable form.
    Input: result of json.load("graphfile")
    Return: predicate information as a tab-separated string line  
    """
    pred = graph["predicate"]
    return "\t".join([pred["displayName"].encode("utf-8"), pred["mention"].encode("utf-8")])
    

if __name__ == '__main__':
    from util import config
    import json
    import os
    from collections import Counter
    
    conf = config.GET_CONF_DICT()
    
    graphsBasePath = conf.get("es.json.graphs")
    for jsonGraphFile in os.listdir(graphsBasePath):
        graph = json.load(open(graphsBasePath + jsonGraphFile))
        questionID = jsonGraphFile.split("-")[1] 
        #print questionID, "\t", get_graph_info(graph)
    
    countPredicates = Counter()
    
    for jsonGraphFile in os.listdir(graphsBasePath):
        graph = json.load(open(graphsBasePath + jsonGraphFile))
        dName = graph["predicate"]["displayName"].encode("utf-8")
        countPredicates[dName] += 1
    for dN in countPredicates.keys():
        print dN, "\t", countPredicates.get(dN)
        
        
        
        
    