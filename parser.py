
import sys

filename = sys.argv[1]
openfile = filename + ".csv"
writefile = filename + "Parsed.csv"
# Using readlines()
file1 = open(openfile, 'r')
count = 0
Lines = ""

while True:
    count += 1

    # Get next line from filec
    line = file1.readline()

    if not line:
        break

    line = line.replace('.0', '')
    line = line.replace(',0', ',')
    print(line)
    Lines += line

file1.close()

# writing to file
file1 = open(writefile, 'w')
file1.writelines(Lines)
file1.close()
