from collections import Counter
import sys, codecs

fen=open("/home/pilatus/WORK/pred-clust/data/ES/en-wp-titles-total-quotes.txt", "r")#, "utf-8")
fes= open("/home/pilatus/WORK/pred-clust/data/ES/es-en-langlinks-quotes-cleaned.txt", "r")#, "utf-8")

out=open("/home/pilatus/WORK/pred-clust/data/ES/gleiche-en.txt", "w")


en_titles = fen.readlines()
es_titles = fes.readlines()

print " en articles: ", len(en_titles)
print " langlinks: ", len(es_titles)

endict = Counter()
excepted = 0
for titl in en_titles:
    try:
        endict[titl] += 1
    except:
        excepted += 1
#         print "full line excepted: " , i
        
occ_counter = Counter()

for item in endict.values():
    occ_counter[item] += 1
    
print "EN \n x-faches Auftreten in y Faellen"

print occ_counter
#print endict
print "======================="

esdict = Counter()
for line in es_titles:
    try:
        titl = line.split("\t")[1]
        esdict[titl] += 1
    except:
        print line
occ_counter = Counter()
for item in esdict.values():
    occ_counter[item] += 1
 
print "ES \n x-faches Auftreten in y Faellen"
print "-------------------------------"
print occ_counter
print "-------------------------------"




enpure = endict.keys()
enlinks = esdict.keys()

print "echte en-Zielartikel: ", len(enlinks)

print "beginne Abgleich"
i = 0
for cand in enlinks:
    i+=1
    print i, 
    if endict.__contains__(cand):
        print "\t gefunden", 
        out.write(cand)
    print " "
    
print "Ende Abgleich"
out.close();

