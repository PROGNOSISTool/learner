from response import ConcreteResponse, Timeout, Undefined
import time
import jpype

waitTime = 0.1
learnerProjectBinPath = "-Djava.class.path=../bin" # path to the java learner setup binaries
mapClass = "sutInterface.tcp.TCPMapper" # mapper class used

class TraceRunner:

    def __init__(self, jvmPath, runNum, skipNum):
        self.jvmPath = jvmPath # path to libjm.so for ubuntu or jvm.dll for windows
        self.runNum = runNum # the number of times the trace is run. 
        self.skipNum = skipNum # the number of lines skipped after each abstract input read
        self.mapper = None # stores the JPype instance of the mapper
        
    def __str__(self):
        return "Trace Runner with parameters: " + str(self.__dict__)
        
    # expected mapper interface:
    #     processOutgoingRequest(string, int, int)
    #     processIncomingResponseComp()
    #     processIncomingTimeout()
        
    def startJava(self):
        print "starting JVM with parameters: " + "-ea " + learnerProjectBinPath
        jpype.startJVM(self.jvmPath, "-ea",learnerProjectBinPath)
    
    def stopJava(self):
        print "shutting down JVM"
        jpype.shutdownJVM()
    
    # get mapper instance
    def getMapper(self):
        if self.mapper is None:
            Mapper = jpype.JClass(mapClass)
            self.mapper = Mapper()
        return self.mapper
    
    # get sender singleton (quite uninspired)
    def getSender(self):
        return self.sender
    
    def processRequest(self,flags, syn, ack):
        return self.getMapper().processOutgoingRequest(flags, syn, ack)
    
    def processResponse(self, response):
        if type(response) is ConcreteResponse:
            responseString = self.getMapper().\
            processIncomingResponseComp(response.flags, str(response.seq), str(response.ack))
        else:
            if type(response) is Timeout:
                self.getMapper().processIncomingTimeout()
            responseString = response.__str__().upper()
        return responseString
    
    def sendConcreteRequest(self, concreteRequest):
        if concreteRequest == "UNDEFINED":
            return Undefined()
        parts = concreteRequest.split()
        flags = str(parts[0])
        syn = long(parts[1])
        ack = long(parts[2])
        return self.getSender().sendInput(flags, syn, ack)
    
    def validReset(self):
        validSeq = self.getMapper().getNextValidSeq()
        self.getSender().sendValidReset(validSeq)
        self.getMapper().setDefault()
    
    # resets by changing ports on the sender.
    def reset(self):
        self.getMapper().setDefault()
        self.getSender().sendReset()
    
    # executes the trace at path. Starting '#' is used to comment the lines.
    def executeTraceFile(self, sender, tracePath):
        self.startJava()
        self.sender = sender
        count = 0
        lineNum = 0
        lastResponse = None # stores the last response abstract response
        
        for i in range(0, self.runNum):
            print 
            print "=== Run Number " + str(i + 1) + " ==="
            print 
            self.reset() # just to make sure everything is well initialized
            
            for line in open(tracePath, "r"):
                lineNum = lineNum + 1
                if count>0:
                    count -= 1
                    continue
                
                line = line.rstrip()
                
                # exit loop at empty line, so empty line acts separator between the trace
                # we are currently executing and the other traces we might have in our file 
                if len(line) == 0:
                    break
                
                # skip comments
                if line[0] == "#":
                    continue
                
                # in case it is a check line, the previous response is matched against the pattern
                # defined after !
                if line[0] == "!":
                    expectedResponse = line[1:]
                    if self.compare(expectedResponse, lastResponse) == False:
                        print "Error line " + str(lineNum) + ": expected " + expectedResponse + " got " + lastResponse 
                        self.shutdown()
                        return 
                    else:
                        continue
                # otherwise the line is an input
                lastResponse = self.processLine(line)
                # after each processed line we skip the following skipNum lines
                count = self.skipNum
        self.shutdown()
        
    def compare(self, expectedResponse, actualResponse):
        matches = False
        expectedResponse = expectedResponse.upper()
        actualResponse = actualResponse.upper()
        expParts = self.splitInputString(expectedResponse)
        actParts = self.splitInputString(actualResponse)
        print "Comparing " + expectedResponse + " with " + actualResponse
        if len(expParts) != len(actParts):
            matches = False
        else:
            if len(expParts) == 4:
                matches = expParts[0] == actParts[0] and\
                (expParts[1] == actParts[1] or expParts[1] == "_") and\
                (expParts[2] ==actParts[2] or expParts[2] == "_")
            else:
                matches = expParts[0] == actParts[0] 
        return matches
        
    # called after all the trace has been process or an exception event occurs 
    # ( the "!" check fails)  
    def shutdown(self):
        self.reset()
        self.getSender().shutdown()
        self.stopJava()
    
    def splitInputString(self, line):
        line = line.replace("(",",");
        line = line.replace(")",",");
        parts = line.split(",")
        return parts
    
    # processes the line constructing the packet/action input, sending it via the sender
    # over the network to the running server and returning the result
    def processLine(self, line):
        print line
        print self.getMapper().getState()
        # in this case we have a normal message
        line = line.replace("(",",");
        line = line.replace(")",",");
        parts = line.split(",")
        
        abstractResponse = None
        # in this case we have a message
        if len(parts) == 4:
            flags = parts[0]
            syn = parts[1]
            ack = parts[2]

            concreteRequest = self.processRequest(flags, syn, ack)
            print self.getMapper().getState()
            concreteResponse = self.sendConcreteRequest(concreteRequest)
            abstractResponse = self.processResponse(concreteResponse)
            
        # in this case we either have reset or  a higher method call
        elif len(parts) == 1: 
            line = line.lower().replace("\n","") # removes excess baggage
            if line == "reset":
                self.reset()
            elif "sendAction" in dir(self.getSender()) and "isAction" in dir(self.getSender()):
                if self.getSender().isAction(line):
                    concreteResponse = self.getSender().sendAction(line)
                    abstractResponse = self.processResponse(concreteResponse)
                else:
                    print "invalid command encountered: " + line
                    self.shutdown()
                    exit(-1)
            else:
                print "the sender of type " + str(type(self.getSender())) + ""\
                " does not implement both sendAction and isAction methods"
                self.shutdown()
                exit(-1) 
        else: 
            print "invalid command encountered: " + line
            self.shutdown()
            exit(-1)
            
        print self.getMapper().getState()
        print abstractResponse
        time.sleep(waitTime)
        return abstractResponse
                
                
    