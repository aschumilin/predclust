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
    targetFile.write("\n".join([",".join([str(value) for value in line]) for line in matrix]))
    targetFile.close()


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