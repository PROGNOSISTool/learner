#from scapy.sendrecv import sr1, sniff
#from scapy.packet import Raw
#from scapy.config import conf
from scapy.layers.inet import IP,TCP
from scapy.all import *  # @UnusedWildImport
from response import Timeout, ConcreteResponse
import platform
from interfaceType import InterfaceType


# variables used to retain last sequence/acknowledgment sent
seqVar = 0
ackVar = 0

# the sender sends packets with configurable parameters to a server and retrieves responses
class Sender:
    # information of the SUT
    def __init__(self, serverMAC=None, serverIP="191.168.10.1", serverPort = 7991,
                 networkInterface="lo", networkInterfaceType=InterfaceType.Ethernet, senderPort=15000, senderPortMinimum=20000,
                 senderPortMaximum=40000, portNumberFile = "sn.txt", useTracking=False,
                 isVerbose=0, waitTime=0.02, resetMechanism=0):
        # data on sender and server needed to send packets 
        self.serverIP = serverIP
        self.serverPort = serverPort
        self.serverMAC = serverMAC
        self.networkInterface = networkInterface
        self.senderPort = senderPort
        self.senderPortMinimum = senderPortMinimum
        self.senderPortMaximum = senderPortMaximum
        self.portNumberFile = portNumberFile;
        
        # time to wait for a response from the server before concluding a timeout
        self.waitTime = waitTime
        
        # use tracking mechanism to aid Scapy
        self.useTracking = useTracking
        
        # TODO: need to remove this, the client should decide on the reset mechanism.
        self.resetMechanism = resetMechanism
        
        #set verbosity (0/1)
        self.isVerbose = isVerbose
        
        if self.useTracking == True:
            # todo see if this limitation is correct 
            if platform.system() != "Linux":
                print("Tracker is not compatible with Windows can only function on Linux")
                self.useTracking = False
            else:
                from tracker import Tracker
                self.tracker = Tracker(self.networkInterface, self.serverIP)
                self.tracker.start()
        else:
            self.tracker = None
    
    def __str__(self):
        return "Sender with parameters: " + str(self.__dict__)

    # chooses a new port to send packets from
    def refreshNetworkPort(self):
        print("previous local port: " + str(self.senderPort))
        self.setSenderPort(self.getNextPort())
        print("next local port: " + str(self.senderPort)+"\n")
        return self.senderPort

    # gets a new port number, an increment of the old. Write new number over the old number in the portNumber file.
    def getNextPort(self):
        f = open(self.portNumberFile,"a+")
        f.seek(0)
        line = f.readline()
        if line == '' or int(line) < self.senderPortMinimum:
            networkPort = self.senderPortMinimum
        else:
            senderPortRange = self.senderPortMaximum - self.senderPortMinimum
            if senderPortRange == 0:
                networkPort = self.senderPortMinimum
            else:
                networkPort = self.senderPortMinimum + (int(line) + 1) % senderPortRange 
        f.closed
        f = open(self.portNumberFile, "w")
        f.write(str(networkPort))
        f.closed
        return networkPort

    # send a packet onto the network with the given parameters, and return the response packet
    # uses scapy to create and send packets
    # response packets are gathered first through scapy's response
    # should scapy return None, then a tracker is used to retrieve whatever packets scapy has missed (in case it did)
    # TODO Why does scapy miss some packets?
    def sendPacket(self,flagsSet, seqNr, ackNr):
        if self.useTracking == True :
            self.tracker.clearLastResponse()
        #if self.isVerbose == 1 :
        packet = self.createPacket(flagsSet, seqNr, ackNr, '')
        response = self.sendPacketAndRetrieveResponse(packet)
        return response
    
    def setServerPort(self, newPort):
        self.serverPort = newPort;
            
    def setSenderPort(self, newPort):
        self.senderPort = newPort
    
    # function that creates packet from data strings/integers
    def createPacket(self, tcpFlagsSet, seqNr, ackNr, payload, destIP = None, destPort = None, srcPort = None,
                     ipFlagsSet="DF"):
        if destIP is None:
            destIP = self.serverIP
        if destPort is None:
            destPort = self.serverPort
        if srcPort is None:
            srcPort = self.senderPort
        print "" +tcpFlagsSet + " " + str(seqNr) + " " + str(ackNr) + " [" + payload + "]"
        pIP = IP(dst=destIP, flags=ipFlagsSet, version=4)
        pTCP = TCP(sport=srcPort,
        dport=destPort,
        seq=seqNr,
        ack=ackNr,
       # window=1,
        flags=tcpFlagsSet)
        if payload == '':
            p = pIP / pTCP
        else:
            p = pIP / pTCP / Raw(load=payload)
        return p
    
    # sends packets and ensures both reception tools are used so as to retrieve the response when such response is given
    # use packet = None for sniffing without sending a packet
    def sendPacketAndRetrieveResponse(self, packet, waitTime = None):
        if waitTime is None:
            waitTime = self.waitTime

        # we need first to clear the last response cached in the tracker 
        if self.useTracking == True :
            self.tracker.clearLastResponse()
        
        scapyResponse = None
        if packet is not None:
            #todo find a more elegant way of finding the client IP?
            self.clientIP = packet[IP].src
            # consider adding the parameter: iface="ethx" if you don't receive a response. Also consider increasing the wait time
            #scapyResponse = sr1(packet, timeout=waitTime, iface=self.networkInterface, verbose=self.isVerbose)
            send([packet], iface=self.networkInterface, verbose=self.isVerbose)
            if self.useTracking:
                # the tracker discards retransmits, but scapy doesn't, so don't use scapy
                scapyResponse = None
        captureMethod = ""
        if scapyResponse is not None:
            response = self.scapyResponseParse(scapyResponse)
            captureMethod = "scapy"
        else:
            response = None
            if self.useTracking == True:
                response = self.tracker.sniffForResponse(self.serverPort, self.senderPort, waitTime)
                #if packet is None:
                #    response = self.tracker.sniffForResponse(self.serverPort, self.senderPort, waitTime)
                #else:
                #    response = self.tracker.getLastResponse(self.serverPort, self.senderPort)
                captureMethod = "tracker"
            else:
                captureMethod = "scapy"
                response = Timeout()

        if captureMethod != "":
            captureMethod = "("+captureMethod+")"
        print response.__str__() + "  "+captureMethod
        if self.useTracking == True:
            self.tracker.clearLastResponse()
        # if scapyResponse is not None:
        #    self.bijectionCheck(scapyResponse, response)
        return response
    
    # transforms a scapy TCP response packet into an abstract response
    def scapyResponseParse(self, scapyResponse):
        flags = scapyResponse[TCP].flags
        seq = scapyResponse[TCP].seq
        ack = scapyResponse[TCP].ack
        concreteResponse = ConcreteResponse(self.intToFlags(flags), seq, ack)
        return concreteResponse
    
    # check if we can reproduce the response packet from the received response 
    def bijectionCheck(self, packet, concreteMessage):
        # first, initialize information that we should now about the sender/server
        sourceIP = packet[IP].src
        sourcePort = packet[TCP].sport
        destIP = packet[IP].dst
        destPort = packet[TCP].dport
        win = packet[TCP].window
        options = packet[TCP].options
        
        # build packets
        result = IP(src=sourceIP,dst=destIP) / TCP(sport=sourcePort, dport=destPort,flags=concreteMessage.flags, seq=concreteMessage.seq, ack=concreteMessage.ack, window=win, options=options)
        del result.chksum
        result = result.__class__(str(result))
        result[TCP].show()
        packet[TCP].show() 
        return True

    # check whether there is a 1 at the given bit-position of the integer
    def checkForFlag(self, x, flagPosition):
        if x & 2 ** flagPosition == 0:
            return False
        else:
            return True

    # the flags-parameter of a network packets is returned as an int, this function converts
    # it to a string (such as "FA" if the Fin-flag and Ack-flag have been set)
    # MAKE SURE the order of checking/appending characters is the same here as it is in the tracker
    def intToFlags(self, x):
        result = ""
        if self.checkForFlag(x, 0):
            result = result + "F"
        if self.checkForFlag(x, 1):
            result = result + "S"
        if self.checkForFlag(x, 2):
            result = result + "R"
        if self.checkForFlag(x, 3):
            result = result + "P"
        if self.checkForFlag(x, 4):
            result = result + "A"
        if self.checkForFlag(x, 5):
            result = result + "U"
        return result
    
    def isFlags(self, inputString):
        isFlags = False
        matchResult = re.match("[FSRPAU]*", inputString)
        if matchResult is not None:
            isFlags = matchResult.group(0) == inputString
        return isFlags

    # tells whether tracking is still active
    def isTracking(self):
        return self.useTracking and (not self.tracker.isStopped())

    # stops the tracking thread (so you don't have to)
    def stopTracking(self):
        self.tracker.stop()

    # uses scapy packet sniffer to sniff whatever TCP/IP packets there are on the network that are of interest
    def sniffPackets(self):
        sniffedPackets = sniff(lfilter=lambda x: IP in x and x[IP].src == self.serverIP and
                                                 TCP in x and x[TCP].dport == self.senderPort,
                               timeout=self.waitTime)
        print 'sniffed'
        return sniffedPackets

    # captures a response without sendin a packet first, in the same way as though a packet was sent
    def captureResponse(self, waitTime=None):
        if waitTime is None:
            waitTime = self.waitTime
        return self.sendInput("nil", None, None, None, waitTime);

    # sends input over the network to the server
    def sendInput(self, input1, seqNr, ackNr, payload, waitTime=None):
        if waitTime is None:
            waitTime = self.waitTime
        # add the MAC-address of the server to scapy's ARP-table to use LAN
        # used every iteration, otherwise the entry somehow
        # w disappears after a while
        # conf.netcache.arp_cache[self.serverIP] = self.serverMAC
        conf.sniff_promisc=False

        timeBefore = time.time()
        
        if input1 != "nil":
            #response = self.sendPacket(input1, seqNr, ackNr)
            ###
            packet = self.createPacket(input1, seqNr, ackNr, payload)
            ###
        else:
            packet = None
        response = self.sendPacketAndRetrieveResponse(packet, waitTime)
        
        # wait a certain amount of time after sending the packet
        timeAfter = time.time()
        timeSpent = timeAfter - timeBefore
        if timeSpent < waitTime:
            time.sleep(waitTime - timeSpent)
        if type(response) is not Timeout:
            global seqVar, ackVar
            seqVar = response.seq;
            ackVar = response.ack;
        return response

    # resets by way of a valid reset. Requires a valid sequence number. Avoids problems encountered with the maximum
    # number of connections allowed on a port.
    def sendValidReset(self,seq):
        if self.resetMechanism == 0 or self.resetMechanism == 2:
            self.sendInput("R", seq, 0, '')
            if self.useTracking == True:
                self.tracker.clearLastResponse()
        if self.resetMechanism == 1 or self.resetMechanism == 2:
            self.sendReset()

    # resets the connection by changing the port number. Be careful, on some OSes (Win 8) upon hitting a certain number of
    # connections opened on a port, packets are sent to close down connections, which affects learning. TCP configurations
    # can be altered, but I'd say in case learning involves many queries, use the other method.
    def sendReset(self):
        self.refreshNetworkPort()
        if self.useTracking == True:
            self.tracker.reset()
            
            
    def shutdown(self):
        if self.useTracking == True:
            self.tracker.stop();

# example on how to run the sender
if __name__ == "__main__":
    print "main test"
    sender = Sender(serverMAC="08:00:27:23:AA:AF", serverIP="131.174.142.227", serverPort=8000, useTracking=False, isVerbose=0, networkPortMinimum=20000, waitTime=1)
    seq = 50
    sender.refreshNetworkPort()
    sender.sendInput("S", seq, 1, '') #SA svar seq+1 | SYN_REC
    sender.sendInput("A", seq + 1, seqVar + 1, '') #A svar+1 seq+2 | CLOSE_WAIT
    sender.sendInput("FA", seq + 1, seqVar + 1, '')
