from time import sleep
__author__ = 'paul,ramon'
import socket
from select import select
import time
import sys
import signal


# The adapter exposes the sender functionality over sockets.
# It  reads packet strings from a socket and sends them to the sender
#     serializes responses retrieved by the sender and writes them back to the socket
# Currently, the adapter 
class Adapter:
    learnerSocket = None
    serverSocket = None
    cmdSocket = None
    sender = None
    data = None
    continous = None

    def __init__(self, socketIP = 'localhost', socketPort = 18200, continuous=True):
        self.socketIP = socketIP
        self.socketPort = socketPort
        self.continuous = continuous

    def __str__(self):
        return "Adapter with parameters: " + str(self.__dict__)

    # returns a new socket to the mapper/learner
    #def setUpSocket(self, commPort, cmdIp, cmdPort):
    def setUpSocket(self, commPort):
        # create an INET, STREAMing socket
        self.serverSocket = socket.socket(
            socket.AF_INET, socket.SOCK_STREAM)
        self.serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        # bind the socket to a public host and a well-known port
        self.serverSocket.bind((self.socketIP, commPort))
        # become a server socket
        self.serverSocket.listen(1)
    
    def establishConnectionWithLearner(self):
        # accept connections from outside
        (clientSocket, address) = self.serverSocket.accept()
        print "learner address connected: " + str(address)
        self.learnerSocket = clientSocket
        
    # closes all open sockets
    # TODO doesn't work all the time 
    def closeSockets(self):
        print self.serverSocket
        print self.learnerSocket
        try:
            try:
                if self.learnerSocket is not None:
                    print "Closing local server socket"
                    self.learnerSocket.close()
            except IOError as e:
                print "Error closing learner socket ", e
            try:
                print(self.serverSocket)
                if self.serverSocket is not None:
                    print "Closing gateway server socket"
                    self.serverSocket.close()
            except IOError as e:
                print "Error closing network adapter socket ", e
            if self.sender is not None:
                try:
                    self.sender.shutdown()
                except Exception as e:
                    print "Error closing sender ", e
            print "Have a nice day"
            time.sleep(1)
            sys.exit(1)
        except KeyboardInterrupt:
            sys.exit(1)
    
    def closeLearnerSocket(self):
        try:
            if self.learnerSocket is not None:
                print "Closing local server socket"
                self.learnerSocket.close()
        except IOError as e:
                print "Error closing learner socket " + e
                
    # reads string from socket until it reads a space/newline
    def receiveInput(self):
        inputstring = '';
        finished = False
        while not finished:
            if not self.data:
                try:
                    ready = select([self.learnerSocket], [], [], 1000)
                    if ready[0]:
                        self.data = self.learnerSocket.recv(1024)
                        if len(self.data) == 0:
                            print "learner closed"
                            return None
                    else:
                        self.fault("Learner socket has been unreadable for too long")
                except IOError:
                    self.fault("No output received from client, closing")
            else:
                c = self.data[0]
                self.data = self.data[1:]
                if c == '\n' or c == ' ':
                    finished = True
                else:
                    inputstring = inputstring + c
        return inputstring
    
    def receiveNumber(self):
        inputString = self.receiveInput()
        if inputString.isdigit() == False:
            #if len(inputString) and inputString[1:].isdigit():
            #    return int(inputString[1:])+2**32
            #else:
                self.fault("Received "+inputString + " but expected a number")
        else:
            return int(inputString)


    # accepts input from the learner, and process it. Sends network packets, looks at the
    # response, extracts the relevant parameters and sends them back to the learner
    def handleInput(self, sender):
        self.sender = sender
        #count = 0
        while (True):
            input1 = self.receiveInput()
            if input1 is None:
                print " Learner closed socket. Closing learner socket."
                self.closeLearnerSocket()
                self.establishConnectionWithLearner()
                continue
            print "received input " + input1
            seqNr = 0
            ackNr = 0
            #count = (count + 1) % 1000
            #if count == 999:
            #    sleep(5)
            
            if input1 == "reset":
                print "Received reset signal."
                self.sender.sendReset()
            elif input1 == "exit":
                msg = "Received exit signal " +  "(continuous" +  "=" + str(self.continuous) + ") :"  
                if self.continuous == False:
                    msg = msg + " Closing all sockets"
                    print msg
                    self.closeSockets()
                    self.sender.sendReset()
                    return
                else:
                    msg = msg + " Closing only learner socket (so we are ready for a new session)"
                    print msg
                    self.closeLearnerSocket()
                    self.establishConnectionWithLearner()
            else:
                print "*****"
                if sender.isFlags(input1):
                    seqNr = self.receiveNumber()
                    ackNr = self.receiveNumber()
                    payload = self.receiveInput()[1:-1]
                    print ("send packet: " +input1 + " " + str(seqNr) + " " + str(ackNr))
                    response = sender.sendInput(input1, seqNr, ackNr, payload);
                elif "sendAction" in dir(self.sender) and self.sender.isAction(input1):
                    print ("send action: " +input1)
                    input1 = input1.lower().replace("\n","")
                    try:
                        response = sender.sendAction(input1) # response might arrive before sender is ready
                    except Exception as e: 
                        print str(e)
                        response = "BROKENPIPE"
                elif input1 == "nil":
                    print("send nothing (nil)")
                    response = sender.captureResponse()
                else:
                    self.fault("invalid input " + input1)
                
                if response is not None:
                    print 'received ' + response.__str__() + "\n"
                    self.sendOutput(response.__str__())
                else:
                    print "received timeout"
                    self.sendOutput("timeout")

    # sends a string to the learner, and simply adds a newline to denote the end of the string
    def sendOutput(self, outputString):
        self.learnerSocket.send(outputString + "\n")
    
    # prints error message, closes sockets and terminates
    def fault(self, msg):
        print '===FAULT EXIT WITH MESSAGE==='
        print msg
        self.closeSockets()
        sys.exit()
    
    # start adapter by list
    def startAdapter(self, sender):
        print "listening on "+str(self.socketIP) + ":" +str(self.socketPort)
        signal.getsignal(signal.SIGINT)
        self.setUpSocket(self.socketPort)
        self.establishConnectionWithLearner()
        self.handleInput(sender)

