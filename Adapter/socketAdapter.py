import socket
from select import select

# adapter over the socket class, not yet used
class SocketAdapter:
    def __init__(self, socket):
        self.adaptedSocket = socket
    
    # reads a stream of characters from adaptedSocket until it reads a space/newline
    def receiveInput(self):
        inputstring = '';
        finished = False
        while not finished:
            if not self.data:
                try:
                    ready = select([self.adaptedSocket], [], [], 3)
                    if ready[0]:
                        self.data = self.adaptedSocket.recv(1024)
                    else:
                        self.fault("Learner adaptedSocket has been unreadable for too long")
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
    
    # reads a number from the adaptedSocket
    def receiveNumber(self):
        inputString = self.receiveInput()
        if inputString.isdigit() == False:
            self.fault("Received "+inputString + " but expected a number")
        else:
            return int(inputString)
    
    def close(self):
        self.adaptedSocket.close()
        
    def send(self, input):
        self.adaptedSocket.send(input)