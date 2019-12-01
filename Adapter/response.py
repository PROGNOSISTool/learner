# Contains classes implementing possible responses.
class Response(object):
    resType = ""
    def __init__(self, resType):
        self.resType = resType
    def __str__(self):
        return "NOT_IMPLEMENTED"
    def hasFlags(self):
        return False

# concrete responses are used if packets are returned
class ConcreteResponse(Response):
    flags = ''
    ack = 0
    seq = 0
    payload = ''
    
    def __init__(self, flags, seq, ack, payload = ''):
        super(ConcreteResponse, self).__init__("CONCRETE")
        self.seq = seq
        self.ack = ack
        self.flags = flags
        self.payload = payload

    def __str__(self):
        outputString = self.flags.replace("U","") + " " + str(self.seq) + " " + str(self.ack) + " [" + str(self.payload) + "]"
        return outputString
    
    def __eq__(self, other):
        equ = False
        if other is not None and type(other) is ConcreteResponse:  
            equ = (self.flags == other.flags) and (self.ack == other.ack) 
            equ = equ and (self.seq == other.seq) and (self.payload == other.payload)
        return equ

    def hasFlags(self):
        return True


# timeouts are used if no packets are returned
class Timeout(Response):
    def __init__(self):
        super(Timeout, self).__init__("TIMEOUT")
    def __str__(self):
        outputString = "timeout"
        return  outputString

# undefineds are used for malformed packets (not used currently) 
class Undefined(Response):
    def __init__(self):
        super(Undefined, self).__init__("UNDEFINED")
    def __str__(self):
        outputString = "undefined"
        return  outputString

if __name__ == "__main__":
    response = ConcreteResponse("ss",10,20,"")
    timeout = Timeout()
    l = [response, timeout]
    l.append(timeout)
    l.append(timeout)
    print len(l)
