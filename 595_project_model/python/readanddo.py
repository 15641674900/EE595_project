import os
import trafficphaseoutput

base_dir = os.getcwd()
file_name = '/Users/zhangjunyao/Desktop/595_project_model/traffic_signal_model/output.txt'

max_episode = 200
max_steps = 3600
phase_Printer = trafficphaseoutput.Phase_Printer()

epi=0
while epi < max_episode:
    step = 0
    index = (step/30)%8
    phase_Printer.print(step,30,index,3600)
    step=step+30

    while step<=max_steps:
        if os.path.exists(file_name):
            with open(file_name, 'r') as file_in:
                content=file_in.readlines()
            if len(content)>0: #check the file is not NULL, ensure the interaction doesn't go wrong
                if (int(content[0]) == step): #check whether the environment is given the up to date data
                    #print(content[1]) #simulate the get data functions
                    index = (step/30)%8 #simulate the action choosing
                    phase_Printer.print(step,30,index,max_steps) #simulate the traffic phase output function
                    step=step+30
        #print("step =",step)
    print("epi=",epi)
    epi += 1
print("double clock file interaction done")
