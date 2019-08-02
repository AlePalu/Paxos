import subprocess
import time

import random

# list of LocalNetworkProcess instances
procList = []

def startNetStack():
    netStack = subprocess.Popen('java -jar build/libs/Paxos-Network.jar', shell=True)
    return netStack

def killNetStack(process):
    # kill the network infrastructure
    killProc(process)
    # kill any process attached to it
    for p in procList:
        killProc(p)
    procList.clear()

def killProc(process):
    process.terminate()
    if process in procList:
        procList.remove(process)

def startClient():
    if(len(procList) < 3):
        client = subprocess.Popen('java -jar build/libs/Paxos-NetworkTest.jar', shell=True)
        procList.append(client)

def killClientAtRandom():
    if len(procList) > 1:
        range = len(procList)
        procToKill = random.randint(0,range-1)
        killProc(procList[procToKill])

if __name__ == "__main__":
    isUp = True
    print("[Tester]: *** bringing up the network infrastructure ***")
    n = startNetStack()
    time.sleep(5)
    print("[Tester]: *** waiting for event ***")
    while(True):
        time.sleep(3)
        # some event will happen now...
        p = random.uniform(0,1)
        
        if p < 0.1 and len(procList) > 0 and isUp: # with 10% probability kill a process(if any)
            print("[Tester]: *** killing a process ***")
            killClientAtRandom()

        if p > 0.1 and p < 0.4 and isUp: # with 30% probability launch a new process
            print("[Tester]: *** launching new process ***")
            startClient()

        if not isUp and p > 0.8: # is node is down, reactivate it with 20% probability
            print("[Tester]: *** reactivating node ***")
            n = startNetStack()
            isUp = True
            p = random.uniform(0,1)
            
        if p > 0.98 and isUp: # with 3% probability kill the entire node
            print("[Tester]: *** killing the entire node ***")
            killNetStack(n)
            isUp = False
