'''
Created on Dec 15, 2014

@author: Artem
'''

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

