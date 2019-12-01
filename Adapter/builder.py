'''
    Used to instantiate all other components via the argparser module.
'''

# the stuff we can build
from sender import Sender
from networkAdapter import Adapter
#from traceRunner import TraceRunner
from actionSender import ActionSender

# the stuff we use to fetch arguments
from argparser import ArgumentParser

# the available arguments
import args

class Builder(object):
    def __init__(self):
        self.argParser = ArgumentParser()
    
    # builds the sender component of the learning setup
    # this can either be a simple packet sender or a more advanced sender, that also has action
    # send capability
    def buildSender(self):
        values = self.argParser.parseArguments(args.senderArguments, "sender")
        # remove the control value which dictates if the sender is simple or is extended with actions
        senderType = values.pop("sendActions")
        simpleSender = Sender(**values) 
        if senderType == False:
            sender = simpleSender
        else:
            actionSender = self.buildActionSender(simpleSender)
            sender = actionSender
        return sender
    
    # builds the actionSender as a wrapper over the original sender component
    def buildActionSender(self, sender):
        values = self.argParser.parseArguments(args.actionSenderArguments, "actionSender")
        values.update({"sender" : sender})
        actionSender = ActionSender(**values)
        return actionSender
    
    # builds the adapter component of the learning setup
    def buildAdapter(self):
        values = self.argParser.parseArguments(args.adapterArguments, "adapter")
        # values = self.getValueMapForArguments(self.adapterArguments, values)
        adapter = Adapter(**values)
        return adapter
    
    # builds the optional trace runner
    def buildTraceRunner(self):
        values = self.argParser.parseArguments(args.runnerArguments, "runner")
        runner = TraceRunner(**values)
        return runner
    