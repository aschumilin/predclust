'''
Created on Dec 20, 2014

@author: Artem
'''
"""
Argument 1 = entities base dir 
Argument 2 = properties target dir 

First: run test.EntExtractorDumper in Java to extract 
entities of each graph into separate files (one per graph). 
This creates files named like: <graphID>.entities.

Second: create set of entities from all graphs.

Third: get incoming+outgoing properties for each entity in set
 
"""
import os
from util import config
from collections import Counter
from baseline.baseline4 import get_candidate_props
from baseline.baseline4 import filter_props
from util.toCSV import listToCSV
import sys
from progress.bar import Bar

def save_props_of_ent(entURI, propsURIList, targetDirPath):
    """
    Save the DBp props under the label of the entity in the specified target dir.
    """
    # take entity label as file name
    fileName = entURI.split("/")[-1]
    
    listToCSV(targetDirPath + fileName, propsURIList)
    
    



if __name__ == '__main__':
    """
    Argument 1 = directory with <graphID>.entities files
    Argument 2 = properties target dir 
    """
    entsSet = Counter()
    try:
        entBaseDir = config.GET_CONF_DICT()["25k-short.ents"]
        #entBaseDir = sys.argv[1]
    except:
        print "Need to provide entBaseDir as argument 1 !"
        exit 
        
    try:
        propsTargetDir = "/home/pilatus/WORK/pred-clust/data/props-shorts-25k/"
        #propsTargetDir = sys.argv[2]
    except: 
        print "Need to provide propsTargetDir as argument 2 !"
        exit
        

    #===========================================================================
    # 1. 
    # add all found ents to dictionary
    #===========================================================================
    for entFile in os.listdir(entBaseDir):
        
        
        for entURI in [entLine.split("\t")[0] for entLine in open(entBaseDir + entFile, "r").readlines()]:
            #print entURI
            
            #===================================================================
            # remove \ characters from the URI !!!!!!!!!!
            # otherwise, query will crash
            #===================================================================
            entURI = entURI.replace("\\", "")

            entsSet[entURI] += 1
    #___ collect all ents from the given graphs
   
    #===========================================================================
    # 2.
    # get dbpedia properties for each entity
    # filter properties by namespace
    # write them to result file: 
    #===========================================================================
    queryProgress = Bar("queries done", max=len(entsSet.keys()))
    
    for entURI in entsSet.keys():     
        #print entURI, #"\n\t", str(len(entPropsList)), " props found"
        try:
            entPropsList = filter_props(get_candidate_props([entURI]))
        except:
            print "exception during sparql query"
            
        if len(entPropsList) > 0:
            save_props_of_ent(entURI, entPropsList, propsTargetDir )
        #else: 
            #print "\t NO PROPS FOUND"
        queryProgress.next()
    #___ query and save props for each entity
    
    queryProgress.finish()
    print len(entsSet), " distinct ents in ", len(os.listdir(entBaseDir)), " graphs"
    