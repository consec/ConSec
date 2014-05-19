import os
import time
from glob import glob
from collections import defaultdict

if __name__ == '__main__':
    db = defaultdict(lambda: None)
    
    while True:
        remake = False
        
        for file in glob('*.rst'):
            mt = os.stat(file).st_mtime
            if mt != db[file]:
                remake = True
            db[file] = mt
        
        if remake:
            os.system('make clean')
            os.system('make html')
        
        time.sleep(1)
