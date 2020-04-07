import os

# phase codes based on environment.net.xml
PHASE_NS_GREEN = 0  # action 0 code 00
PHASE_NS_YELLOW = 1
PHASE_NSL_GREEN = 2  # action 1 code 01
PHASE_NSL_YELLOW = 3
PHASE_EW_GREEN = 4  # action 2 code 10
PHASE_EW_YELLOW = 5
PHASE_EWL_GREEN = 6  # action 3 code 11
PHASE_EWL_YELLOW = 7

class Phase_Printer:
    def __init__(self):
        self._currentstep = 0
        self._duration = 0
        self._phaseindex = 0
        self._maxsteps = 0

    def print(self, currentstep, duration, phaseindex, max_steps):
        self._currentstep = currentstep
        self._duration = duration
        self._phaseindex = phaseindex
        self._maxsteps = max_steps
        if self._currentstep + self._duration > self._maxsteps:
            self._duration = self._maxsteps - self._currentstep
        base_dir = os.getcwd()
        file_name = '/Users/zhangjunyao/Desktop/595_project_model/traffic_signal_model/src/main/resources/SignalPhases/input.txt'
        file_out=open(file_name,"w")
        print(self._currentstep+self._duration,file=file_out)
        #print(self._currentstep,file=file_out)
        if self._phaseindex == 0:
            file_out.write("RRRGGGRRRRRR\n")
        if self._phaseindex == 1:
            file_out.write("GGGRRRRRRRRR\n")
        if self._phaseindex == 2:
            file_out.write("RRRRRRRRRGGG\n")
        if self._phaseindex == 3:
            file_out.write("RRRRRRGGGRRR\n")
        if self._phaseindex == 4:
            file_out.write("RRRRRRRRRRRR\n")
        if self._phaseindex == 5:
            file_out.write("YYYYYYYYYYYY\n")
        if self._phaseindex == 6:
            file_out.write("RRRRRRRRRRRR\n")
        if self._phaseindex == 7:
            file_out.write("RRRRRRGGGRRR\n")
        
        file_out.close()
'''
max_steps = 3600
i = 0 
while i< max_steps:
    phase_Printer = Phase_Printer()
    index = i%30
    phase_Printer.print(i,30,index,3600)
    i+=30
'''