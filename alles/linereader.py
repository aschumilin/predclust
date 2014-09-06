from collections import Counter
import sys

f=open("/home/pilatus/WORK/pred-clust/data/ES/es-en-langlinks.txt", "r")
out=open("/home/pilatus/WORK/pred-clust/data/ES/ok_es-en-langlinks.txt", "w")


lines = f.readlines()

# x ='hallo"world\\"'
# anf = x.index("\\")
# print anf

chardict = Counter()
badlines = []

i = 0
for l in lines:
    anf = -1
    try:
        anf = l.index("\\")
        
    except:
#         print "err:", sys.exc_info()[0]
        i+=1
        
    if anf > -1:
        subs = l[anf:anf+2]
#         print subs
        chardict[subs] += 1
        badlines.append(l)
        
        lnew = l.replace('\\"', '"')
        lnew = lnew.replace("\\'", "'")
        out.write(lnew)
        
        if subs=='\\"':
            print l
    else:
        out.write(l)
                
        
        
        
print chardict
print len(lines)
print "articles without special chars: ", len(badlines) 
f.close()
out.close()