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
    return VALUES