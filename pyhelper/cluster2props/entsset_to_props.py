'''
Created on Dec 20, 2014

@author: Artem
'''
"""
Argument 1 = properties target dir 

First: run test.EntExtractorDumper in Java to extract 
entities of each graph into separate files (one per graph). 
Get files <graphID>.entities.

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
    Argument 1 = properties target dir 
    """
    entsSet = Counter()
    
    entBaseDir = config.GET_CONF_DICT()["25k-long.ents"]
    try:
        propsTargetDir = sys.argv[1]
    except: 
        print "Need to provide propsTargetDir as argument !"
        exit
        
    j = 0
    #===========================================================================
    # 1. 
    # add all found ents to dictionary
    #===========================================================================
    for entFile in os.listdir(entBaseDir):
        j += 1
        if j>=15: break
        
        for entURI in [entLine.split("\t")[0] for entLine in open(entBaseDir + entFile, "r").readlines()]:
            #print entURI
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
        entPropsList = filter_props(get_candidate_props([entURI]))
        
        print entURI, "\n\t", str(len(entPropsList)), " props found"
        #print entPropsList
        save_props_of_ent(entURI, entPropsList, propsTargetDir )
        queryProgress.next()
    #___ query and save props for each entity
    
    queryProgress.finish()
    print len(entsSet), " distinct ents in ", len(os.listdir(entBaseDir)), " graphs"
    