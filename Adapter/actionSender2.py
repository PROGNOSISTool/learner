#from socketAdapter import SocketAdapter
import socket
from time import sleep
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
               "send"]
    def __init__(self, cmdIp = "192.168.56.1", cmdPort=5000, sender = None):
        self.cmdPort = cmdPort
        self.cmdIp = cmdIp
        self.sender = sender
        
    def __str__(self):
        ret =  "ActionSender with parameters: " + str(self.__dict__)
        if self.sender is not None:
            ret  = ret + "\n" + str(self.sender)
        return ret
        
    # returns a new socket to the mapper/learner
    def setUpSocket(self):
        if self.cmdSocket is None:
            cmdSocket = socket.create_connection((self.cmdIp, self.cmdPort))
            cmdSocket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            cmdSocket.settimeout(60)
            print "python connected to server Adapter at " + self.cmdIp + " " + (str(self.cmdPort))
            #self.cmdSocket = SocketAdapter(cmdSocket)
            self.cmdSocket = cmdSocket
            self.sockFile = self.cmdSocket.makefile('r')
        self.listenForServerPort()
        
    
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
            print "received " + line
            line = line.split()
            #if line == "":
            #    self.cmdSocket.close()
            #    self.cmdSocket = None
            #    sleep(1)
            #    self.setUpSocket()
            #    print "Observed crash"
            if line[0] != "port":
                raise Exception("expected 'port', received '" + line[0] + "'")
                self.serverPort = int(line[1])
                print "next server port: " + str(self.serverPort)
#            word = self.sockFile.readline()
#            newPortString = self.cmdSocket.recv(1024)
#             print "received " + newPortString
#             for word in newPortString.split(): # TODO check if this really always works with a stream
#                 if newPortFound:
#                     self.serverPort = int(word)
#                     print "next server port: " + word
#                     return
#                 if word == "port":
#                     newPortFound = True
                    
    def sendReset(self):
        if self.cmdSocket is None:
            self.setUpSocket()
        print "reset"
        print "********** reset **********"
        self.cmdSocket.send("reset\n")
        if syn_ok:
            line = self.sockFile.readline()
            if line != "ok\n":
                raise Exception("expected 'ok' upon reset, received '" + line + "'")
        self.listenForServerPort()
        self.sender.setServerPort(self.serverPort)
    
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
        if self.isAction(inputString):
            self.cmdSocket.send(inputString + "\n")
            if syn_ok:
                ok = self.sockFile.readline()
                if ok != "ok\n":
                    raise Exception("expected 'ok', received '" + ok + "'")
                self.cmdSocket.send("ok\n")
            response = self.sender.captureResponse()
#             for word in ok.split(): # TODO check if this really always works with a stream
#                 if word == "ok":
#                     self.cmdSocket.send("ok\n")
#                 else:
#                     raise Exception("expected 'ok', received" + word)
#             response = self.sender.captureResponse()
            #cmdResponse = self.cmdSocket.recv(1024)
            #print "server adapter response: " + cmdResponse
        else:
            print inputString + " not a valid action ( it is not one of: " + str(self.actions) + ")"
        return response
                
    def sendInput(self, input1, seqNr, ackNr, payload):
        return self.sender.sendInput(input1, seqNr, ackNr, payload)
    
    def shutdown(self):
        self.closeSockets()
        if self.sender is not None:
            self.sender.shutdown()


    