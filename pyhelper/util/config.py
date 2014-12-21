'''
Created on Dec 17, 2014

@author: Artem

'''
def GET_CONF_DICT():
    VALUES = dict()
    VALUES.update({"es.json.graphs" : 
                   "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/BaselineX/qald-es-graphs-json/"})
    VALUES.update({"en.json.graphs" :
                   "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/BaselineX/qald-en-graphs-json/"})
    VALUES.update({"qald.base" : 
                   "/home/pilatus/Dropbox/AIFB/09_Predicate_Clustering/BaselineX/qald4-graphs/"}) 
    VALUES.update({"25k-long.ents":
                   "/home/pilatus/WORK/pred-clust/data/entities-longarticles-25k/"})
    VALUES.update({"25k-short.ents":
                   "/home/pilatus/WORK/pred-clust/data/entities-shorts-25k/"})
    VALUES.update({"sparql.endpoint":
                   "http://aifb-ls3-merope.aifb.kit.edu:8890/sparql"})
    
    return VALUES