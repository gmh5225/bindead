#!/usr/bin/python
import re
import sys

def main():
    """ Modify a file to replace shr followed by shl by an and instruction.
        The latter is easier for us to handle when e.g. the stack pointer is aligned.
        The file is modified in place.
        Args:
            filename is taken from command line
    """
    fileName = sys.argv[1]
    inputFile = open(fileName)
    pattern = re.compile(r".*shr\s*(eax), (\d).*\n.*(shr|sal)\s*\1, \2.*")
    fileContent = inputFile.readlines()
    newFileContent = []       
    matched = False
    i = 0                                                                                                     
    while i < len(fileContent):
        currentLine = fileContent[i]
        if i + 1 == len(fileContent):
            newFileContent.append(currentLine)
            break
        nextLine = fileContent[i + 1]                      
        match = pattern.match(currentLine + nextLine)                                                                                                 
        if match:
            matched = True
            i = i + 1
            shiftImmediate = int(match.group(2))
            andImmediate = (2 ** shiftImmediate) * (-1)
            comment = "# replaced shifts used for stack pointer alignment below with and instruction\n"
            comment = comment + "# " + currentLine
            comment = comment + "# " + nextLine
            currentLine = comment + "\tand\t" + match.group(1) + ", " + str(andImmediate) + "\n"                                                                                                                   
        i = i + 1
        newFileContent.append(currentLine)          
    inputFile.close()
    if matched:
        outputFile = open(fileName, "w")
        outputFile.writelines(newFileContent)
    
if __name__ == '__main__':
    main()
