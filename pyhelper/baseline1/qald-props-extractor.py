'''
Created on Dec 3, 2014

@author: Artem
'''

# SPARQL query parser
import fyzz
import codecs
import re
import xml.etree.ElementTree as ET

    
if __name__ == '__main__':
    qaldTrainingFilePath = "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/QALD_challenge/qald-4-data/qald-4_multilingual_train_withanswers.xml"
    qaldRawFile = codecs.open(qaldTrainingFilePath, "r")
    
    # find all <query> elements
    queries = ET.parse(qaldRawFile).getroot().findall(".//question/query")
    #print "found ", str(len(queries)), " queries"
    
    i=0
    for q in queries:
        qtext = q.text#.replace("(", "").replace(")", "").replace("COUNT", "")
        try:
            # WHERE clause consists of triples
            whereStatement = fyzz.parse(qtext).where
            
            # predicate (property) is the second item in the tuple
            # it consists of a prefix and the actual property
            for trip in whereStatement:
                print trip[1]
            
        except:
            if "WHERE" not in qtext:
                i+=1
                #print qtext
    #print i, " queries without a WHERE clause"
        
    