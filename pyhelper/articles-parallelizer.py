from pymongo import MongoClient
import codecs

def get_docs_coll(collname):
    
    mongoHost = "romulus"
    mongoPort = 22222
    dbName = "all"
    collName = collname
    user = "atsc"
    pwd = "123"
    
    client = MongoClient(mongoHost, mongoPort)
    db = client[dbName]
    db.authenticate(user, pwd)
    
    return db[collName]

def parallelize():
    
    dir = "/home/pilatus/WORK/pred-clust/data/ES-EN-parallel-articles/"
    # spanische ID - engl. Titel
    esFile = dir+"es-en-langlinks-no-escapes.txt"
    
    # engl. Titel - engl. ID
    enFile = dir+"en-title-id-all.txt"
    
    resFileName = dir+"es-en-parallel-ID-PAIRS.txt"
    
    
    esL = open(esFile, "r").readlines()
    enL = open(enFile, "r").readlines()
    resFile = open(resFileName, "w")
    
    esDict = {}
    enDict = {}
    

    print "reading spanish dict"
    for line in esL:
        parts = line.strip().split("\t")
        if len(parts) is 2:
            esDict.update({parts[0] : parts[1]})


    print "reading english"
    for line in enL:
        parts = line.strip().split("\t")
        if len(parts) is 2:
            enDict.update({parts[0] : parts[1]})
    
    print "getting the parallel ones"
    for esID in esDict.keys():
        enTitle = esDict.get(esID)
        enID = enDict.get(enTitle)
        
        if enID is not None:
            # get enID and write line with esID - enID
            line = esID + "\t" + enID + "\n"
            resFile.write(line)
    resFile.close()
    
    print "done"


if __name__ == '__main__':
    
    """ # read and save english IDs and titles 
    docs = get_docs_coll("docs")
    
    resfile = codecs.open("/home/pilatus/WORK/pred-clust/data/ES-EN-parallel-articles/en-title-id-all.txt", "w", "utf-8")
    
    print "reading db to dict"
    dall = {}
    
    doneSrl = docs.find({"lang":"en"}, {"docTitle":1, "docId":1})
    for doc in doneSrl:                    
        id = doc.get("docId")
        title = doc.get("docTitle")
        line = title + "\t" + id + "\n"
        resfile.write(line)
    
    resfile.close()
    """
    parallelize()
    

