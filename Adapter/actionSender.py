#from socketAdapter import SocketAdapter
import socket
import time
from _socket import MSG_DONTWAIT, MSG_PEEK
syn_ok = False

# extends sender functionality with higher level commands
class ActionSender:
    cmdSocket = None
    sender = None
    actions = ["listen",
               "accept",
               "close",
               "closeconnection",
               "closeserver",
               "exit",
               "closeclient",
               "connect",
               "send",
               "rcv",
                "nil"]
    count = None
    def __init__(self, cmdIp = "192.168.56.1", cmdPort=5000, sender = None):
        self.cmdPort = cmdPort
        self.cmdIp = cmdIp
        self.sender = sender
        self.count = 0
        
    def __str__(self):
        ret =  "ActionSender with parameters: " + str(self.__dict__)
        if self.sender is not None:
            ret  = ret + "\n" + str(self.sender)
        return ret
        
    # returns a new socket to the mapper/learner
    def setUpSocket(self):
        if self.cmdSocket is None:
            print (self.cmdIp, self.cmdPort)
            try:
                cmdSocket = socket.create_connection((self.cmdIp, self.cmdPort))
            except:
                print "connection refused"
                print "sleeping and attempting to establish connection again"
                time.sleep(0.5)
                socket.create_connection((self.cmdIp, self.cmdPort))
            cmdSocket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            cmdSocket.settimeout(60)
            print "python connected to server Adapter at " + self.cmdIp + " " + (str(self.cmdPort))
            #self.cmdSocket = SocketAdapter(cmdSocket)
            self.cmdSocket = cmdSocket
            self.sockFile = self.cmdSocket.makefile('r')
    
    def closeSockets(self):
        try:
            if self.cmdSocket is not None:
                print "Telling server adapter to end session"
                self.cmdSocket.send("exit\n")
                print "Closing server adapter command socket"
                self.cmdSocket.close()
        except IOError as e:
            print "Error closing ActionSender " + e
            raise e
    
    # fetches a server port
    def listenForServerPort(self):
#        while True:
            line = self.sockFile.readline()
            if line == "":
                self.cmdSocket.close()
                self.cmdSocket = None
                print "Setting up socket"
                self.setUpSocket()
                time.sleep(0.3)
                #self.sendReset()
                line = self.sockFile.readline()
            print "received " + line
            line = line.split()
            if line[0] != "port":
                raise Exception("expected 'port', received '" + line[0] + "'")
            self.serverPort = int(line[1])
            print "next server port: " + str(self.serverPort)
                    
    def sendReset(self):
        try:
            self.checkAlive()
        except: 
            pass
        if self.cmdSocket is None:
            self.setUpSocket()
            self.listenForServerPort()
            self.sender.setServerPort(self.serverPort)
            return 
        print "reset"
        print "********** reset **********"
        self.count = self.count + 1
        if self.count % 10 == 0:
            self.restartAdapter()
        else:
             self.resetAdapter()
        self.listenForServerPort()
        self.sender.setServerPort(self.serverPort)

    def resetAdapter(self):
        self.cmdSocket.send("reset\n")    
    def restartAdapter(self):
        self.cmdSocket.send("exit\n")
        if syn_ok:
            line = self.sockFile.readline()
            if line != "ok\n":
                raise Exception("expected 'ok' upon reset, received '" + line + "'")
        time.sleep(0.4)
        self.setUpSocket()        

    def captureResponse(self):
        response = None
        if self.sender is not None:
            response = self.captureResponse()
        return response
    
    def isFlags(self, inputString):
        isFlags = False
        if self.sender is not None:
            isFlags = self.sender.isFlags(inputString)
        return isFlags
    
    def isAction(self, inputString):
        return inputString in self.actions
    
    def sendAction(self, inputString):
        print "sending action ", inputString
        if self.isAction(inputString):
            #if inputString == "nil":
            #self.cmdSocket.recv(1000, MSG_DONTWAIT)
            if inputString != "nil":
                self.cmdSocket.send(inputString + "\n")
            if syn_ok:
                ok = self.sockFile.readline()
                if ok != "ok\n":
                    raise Exception("expected 'ok', received '" + ok + "'")
                self.cmdSocket.send("ok\n")
            waitForAns = self.sender.waitTime 
            if inputString in ["connect"]:
                waitForAns = waitForAns + 0.1
            if inputString in ["send"]:
                waitForAns = waitForAns + 0.01            
            if inputString in ["close"]:
                waitForAns = waitForAns + 0.1            
            response = self.sender.captureResponse(waitTime=waitForAns)
            #print "server adapter response: " + cmdResponse
        else:
            print inputString + " not a valid action ( it is not one of: " + str(self.actions) + ")"
        self.checkAlive()
        return response
    def checkAlive(self):
        return
#        try:
#            print "receiving"
#            self.cmdSocket.send("probe\n")
#            time.sleep(0.1)
#            self.cmdSocket.send("probe\n")
            #self.cmdSocket.recv(1, MSG_DONTWAIT | MSG_PEEK)
#            print "received"
#        except:
#            print "He died"
#            self.cmdSocket.close()
#            self.cmdSocket = None
#            raise Exception("broken pipe")
    def sendInput(self, input1, seqNr, ackNr, payload):
        return self.sender.sendInput(input1, seqNr, ackNr, payload)
    
    def shutdown(self):
        self.closeSockets()
        if self.sender is not None:
            self.sender.shutdown()


    
