'''
Created on Dec 15, 2014

@author: Artem
'''

from util import config
def send_query(sparqlQueryString, listOfVarNames):
    from SPARQLWrapper import SPARQLWrapper, JSON
    """
    Query DBpedia with a simple SELECT DISTINCT ?x?y ... WHERE {} 
    Input: sparql query string
    Return: list of result tuples [(xValue, yValue), (), ...]  
    """
    #sparql = SPARQLWrapper("http://dbpedia.org/sparql")
    sparql = SPARQLWrapper(config.GET_CONF_DICT()["sparql.endpoint"])
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

