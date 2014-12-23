'''
Created on Dec 14, 2014

@author: Artem
'''
def listToCSV(targetFilePath, matrix, headerString=""):
    """
    write given matrix as csv file with given header
    """   
    targetFile = open(targetFilePath, "w")
    if headerString is not "":
        targetFile.write(headerString + "\n")   
    
    if len(matrix) > 0:
        test = matrix[0] # sample element of matrix
        
        if type(test) is tuple:
            #print "tuple"
            targetFile.write("\n".join([",".join([str(value) for value in line]) for line in matrix]))
        elif type(test) is list: 
            #print "list"
            targetFile.write("\n".join([",".join([str(value) for value in line]) for line in matrix]))
        else:
            #print "other type"
            targetFile.write("\n".join([line.strip() for line in matrix]))
    else:
        print "exception when writing matrix to file: ", targetFilePath, "\n"
    
    targetFile.close()

if __name__ == '__main__':
    lt = [('a1', 'a2', 'a3'), ('b1', 'b3')]
    ll = [['abc1', 'adf2', 'ajk3'], ['bre1', 'bta2', 'bod3']]
    l = ["hallo", "bye"]
    listToCSV("/home/pilatus/WORK/pred-clust/data/testLT", lt)
    listToCSV("/home/pilatus/WORK/pred-clust/data/testLL", ll)
    listToCSV("/home/pilatus/WORK/pred-clust/data/testL", l)
    
    
def tupleListToCSV(targetFilePath, tupleList, headerString=""):
    
    """
    write each tuple as separate comma separated line
    input: list of tuples  [(a,b,..), ...]
    """
    targetFile = open(targetFilePath, "w")
    if headerString is not "":
        targetFile.write(headerString + "\n")
    targetFile.write("\n".join([",".join([str(value) for value in tupl]) for tupl in tupleList]))
    targetFile.close()   