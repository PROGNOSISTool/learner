__author__ = 'paul,ramon'

"""
   run a sequence of abstract inputs from a file using the Java Mapper in the learner project. 
   Currently, it runs the trace found in "trace.txt"

   usage: sudo python debug.py [traceRunner param] [sender param] [actionSender param]
     
   For parameters for each component, see args.py
   
    Example:
    
    file trace.txt:
    SYN(INV, INV)
    ACK(V, V)
    SYN(V, V)
    ...
    
"""

from builder import Builder

# debug main. Uses the trace runner to run individual traces
if __name__ == "__main__":
    print "==Preparation=="
    builder = Builder()
    
    print "\n==Sender Setup=="
    sender = builder.buildSender()
    print str(sender)
     
    print "\n==Trace Runner Setup=="
    runner = builder.buildTraceRunner()
    print str(runner)
    
    tracePath = "../input/trace.txt"
    print "\n==Running Trace File " + tracePath + " =="
    runner.executeTraceFile(sender, tracePath)