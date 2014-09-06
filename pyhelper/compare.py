from collections import Counter
import sys

llfile=open("/home/pilatus/WORK/pred-clust/data/ES/es-en-langlinks-no-escapes.txt", "r")
srlfile=open("/home/pilatus/WORK/pred-clust/data/ES/en-articles-no-quotes.txt", "r")

lls = llfile.readlines()
arts = srlfile.readlines()

print " lang-linked files: ", len(lls)
print " srl'd files: ", len(arts)

lldict = Counter()
excepted = 0
for i in lls:
    try:
        title = i.split("\t")[1]
#         print title
        lldict[title] += 1
    except:
        excepted += 1
#         print "full line excepted: " , i
        
    

print "ll scan ready. excepted: ", excepted

for j in arts:
#     print j
    lldict[j] += 1

print "arts scan ready" 

# print lldict
zweier = 0
num = 0
for k in lldict:
    num = lldict.get(k)
    if num>1:
        if num == 2:
            print k
            zweier += 1
#         print num , "  ", k
        
print "zweier: ", zweier      