'''
Created on Dec 14, 2014

@author: Artem
'''
from xml.etree import ElementTree as ET
from xml.etree.ElementTree import Element as E
import os
from util.toCSV import tupleListToCSV

def get_ents_list(annotationsXMLRoot):
    """
    return list of tuples of entities [(url, mention, weight, from, to), ...] 
    """
    topics = annotationsXMLRoot.findall(".//DetectedTopic")
    #print str(len(topics))
     
    return [(entity.attrib["URL"].encode("utf-8"), 
               entity.attrib["mention"].encode("utf-8"), 
               entity.attrib["weight"].encode("utf-8"), 
               entity.attrib["from"].encode("utf-8"), 
               entity.attrib["to"].encode("utf-8")) for entity in topics]

    
if __name__ == '__main__':
    
    baseDir = "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/BaselineX/qald4-graphs/"
    
    for qdir in os.listdir(baseDir):
        qid = qdir.split("-")[-1]
        for lang in ["en", "es"]:
            annotFileName = "".join([baseDir, qdir, "/", lang, "-", qid, "-annot.xml"])
            ents = get_ents_list(ET.parse(annotFileName).getroot())
            tupleListToCSV(annotFileName[0:-3] + "extracted", ents)
            
            
            
        
    
    
    #entsTuples = get_ents_list(ET.parse("/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/BaselineX/qald4-graphs/en-2_es-2/es-2-annot.xml").getroot())
    
    