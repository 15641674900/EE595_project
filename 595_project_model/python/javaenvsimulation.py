import os
import random

max_episode = 200
max_steps=3600
steptodo=30

base_dir = os.getcwd()
file_out_name = os.path.join(base_dir,'/Users/zhangjunyao/Desktop/595_project_model/traffic_signal_model/src/main/resources/SignalPhases/input.txt')
file_in_name = os.path.join(base_dir,'/Users/zhangjunyao/Desktop/595_project_model/traffic_signal_model/output.txt')

epi=0
while epi < max_episode:
    #initial vehicle conditions
    step=0
    file_out=open(file_out_name,"w")
    print("0",file=file_out)
    print(random.randint(0,100),random.sample(["N","W","S","E"],1),file=file_out)
    file_out.close()

    while step+steptodo<=max_steps:
        if os.path.exists(file_in_name):
                with open(file_in_name, 'r') as file_in:
                    content=file_in.readlines()
                if len(content)>0:
                    while (int(content[13]) > step) & (int(content[13]) < (step+100)): #ensure there will not be problems caused by different speed of clocks
                        #eg. the content[13] == 3600 (last episode) and not update to 0 yet, then the step should not continue to +1
                        step += 1 #do steps
                        #print(step)
                    file_out=open(file_out_name,"w")
                    print(step,file=file_out)
                    print(random.randint(0,100),random.sample(["N","W","S","E"],1),file=file_out)
                    file_out.close()

    print("epi=",epi,"step=",step)

    if step == max_steps:
        epi+=1

print("environment done")
