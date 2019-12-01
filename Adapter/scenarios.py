__author__ = 'paul'
from sender import *

# contains various scenarios I analyzed in the past. With the traceRunner tool developed,
# this way of handling has become deprecated. 

# SA 55 1
# SA 1854262311 56
# S 56 1854262312
# SA 1220472750 57
def scenario3():
    seq = 55
    sender = Sender(useTracking=True, isVerbose=1);
    sender.sendReset()
    sender.sendInput("SA", seq, 1) #SA svar seq+1 | SYN_REC
    sender.sendInput("S", seq + 1, seqVar + 1) #A svar+1 seq+2 | CLOSE_WAIT


# S 55 1
# SA 3000004891 56
# FA 56 3000004892
# A 3000004892 57
# SA 57 3000004893
# timeout
# S 57 3000004893
# SA 3002936060 58
def scenario1():
    seq = 55
    global isVerbose, seqVar
    isVerbose = 1
    sender = Sender(useTracking=True, isVerbose=1);
    sender.sendReset()
    sender.sendInput("S", seq, 1) #SA svar seq+1 | SYN_REC
    sender.sendInput("FA", seq + 1, seqVar + 1) #A svar+1 seq+2 | CLOSE_WAIT
    sender.sendInput("SA", seq + 2, seqVar + 2) # [AUS] RST svar+2 | CLOSED
# S 55 1
# SA 3228886825 56
# FA 56 3228886826
# A 3228886826 57
# A 57 3228886827
# SA 3307790927 56
def scenario2():
    seq = 55
    global seqVar
    sender = Sender(useTracking=True, isVerbose=1);
    sender.sendReset()
    sender.sendInput("S", seq, 1) #SA svar seq+1 | SYN_REC
    sender.sendInput("FA", seq + 1, seqVar + 1) #A svar+1 seq+2 | CLOSE_WAIT
    sender.sendInput("A", seq + 2, seqVar + 1) # [AUS] RST svar+2 | CLOSED

# target SYN+ACK(V,V)/timeout transition
def scenario4():
    seq = 55
    global seqVar
    sender = Sender(useTracking=False, isVerbose=1);
    sender.sendReset()
    sender.sendInput("S", seq, 1) #SA svar seq+1 | SYN_REC
    sender.sendInput("SA", seq + 1, seqVar + 1) #A svar+1 seq+2 | CLOSE_WAIT


#S 256873882 2325711926
#SA 2359696765 256873883
#S 0 2359696766
#SA 2359696765 256873883

# target SYN+ACK(V,V)/timeout transition
def scenario5():
    seq = 256873882
    global seqVar
    sender = Sender(useTracking=True, isVerbose=1);
    sender.sendReset()
    sender.sendInput("S", seq, 1) #SA svar seq+1 | SYN_REC
    sender.sendInput("S", 0, 30) #A svar+1 seq+2 | CLOSE_WAIT
# it believes SYN/ACK wasn't received, so decides to retransmit it.
# second SYN with INV INV always leads to re-transmission

# S 2195024002 4027050663
# SA 2012154486 2195024003
# R 896739962 3006125133
# timeout
# SA 1907624083 1547371810
# timeout
# FA 2804750325 3517440703
# timeout
# F 3509051224 404543837
# A 2012154487 2195024003
# R 207753863 3470850501
# timeout
# R 1912718391 3547724878
# timeout
# FA 1111900754 809960323
# timeout
# SA 2370638902 1120973000
# timeout
# A 3519400106 1291738072
# timeout
# SA 2682686870 81888676
# timeout
# F 2375991952 447076209
# A 2012154487 2195024003
# FA 2195024003 2012154488
# SA 2012154486 2195024003
def scenario6():
    seq = 256873882
    global seqVar
    sender = Sender(useTracking=True, isVerbose=1);
    sender.sendReset()
    sender.sendInput("S", seq, 1)
    sender.sendInput("RA", seq + 1, 334)
    #sender.sendInput("SA", )

# S 4182706173 2198067953
# A 1324802308 4092203593
# A 0 1324802309
# A 1324802308 4092203593
def scenario7():
    seq = 4182706173
    global seqVar
    sender = Sender(useTracking=True, isVerbose=1)
    sender.sendReset()
    print(sender.sendInput("S", seq, 2198067953).ack)

    #sender.sendInput("A", seq + 1, 334)

def scenario8():
    seq = 4294967295
    global seqVar
    sender = Sender(useTracking=True, isVerbose=1)
    sender.sendReset()
    sender.sendInput("S", seq, 0)


# S 341566692 277277600
# SA 2738973448 341566693
# R 764295758 2738973449
# timeout
# S 1218267894 1420096704
# SA 2738973448 341566693
def scenario9():
    seq = 1200;
    global seqVar
    sender = Sender(useTracking=True, isVerbose=1)
    sender.sendReset()
    sender.sendInput("S", seq, 0);
    sender.sendInput("R", seq+1, seqVar+1);
    sender.sendInput("S", seq-100, 9090);

# S 1923506209 1980570823
# SA 3975181246 1923506210
# F 2536736535 3518618248
# A 3975181247 1923506210
def scenario10():
    seq = 120034;
    global seqVar
    sender = Sender(useTracking=True, isVerbose=1)
    sender.sendReset()
    sender.sendInput("S", seq, 10)
    sender.sendInput("F", seq+100, seqVar+100)
    sender.sendReset()
    sender.sendInput("S", seq, 10)
    sender.sendInput("F", seq+100, seqVar+100)
    sender.sendReset()
    sender.sendInput("S", seq, 10)
    sender.sendInput("F", seq+100, seqVar+100)
    sender.sendReset()
    sender.sendInput("S", seq, 10)
    sender.sendInput("F", seq+100, seqVar+100)
    sender.sendReset()
    sender.sendInput("S", seq, 10)
    sender.sendInput("F", seq+100, seqVar+100)
    sender.sendReset()
    sender.sendInput("S", seq, 10)
    sender.sendInput("F", seq+100, seqVar+100)
    sender.sendReset()
    sender.sendInput("S", seq, 10)
    sender.sendInput("F", seq+100, seqVar+100)
    sender.sendReset()
    sender.sendInput("S", seq, 10)
    sender.sendInput("F", seq+100, seqVar+100)

# =========Trace 150=========
# S 637250740 2233230871
# SA 1797254657 637250741
# F 3711991173 1797254658
# A 1797254658 637250741
# S 637250741 1797254659
# RA 0 637250742
# S 2783591730 403749488
# SA 1797267031 2783591731
#
#
# SYN(V,V)
# SYN+ACK(V,V)
# FIN(INV,V)
# ACK(V,V)
# SYN(V,V)
# RST+ACK(V,V)
# SYN(V,V)
# SYN+ACK(V,V)
#
# =========Trace 151=========
# S 2036987655 3265500663
# SA 4266394843 2036987656
# F 2240251580 4266394844
# A 4266394844 2036987656
# A 2036987656 4266394845
# R 4266394845 4266394845
#
#
# SYN(V,V)
# SYN+ACK(V,V)
# FIN(INV,V)
# ACK(V,V)
# ACK(V,V)
# RST(V,V)
#

# S 2616307222 10
# SA 1480899272 2616307223
# F 2616307322 1480899273
# timeout
# A 2616307223 1480899273
# timeout
# S 2616307122 1480899172
# A 1480899273 2616307223

# =========Trace 152=========
# S 3765233380 3045359813
# SA 3383820359 3765233381

# F 2361382207 3383820360
# A 3383820360 3765233381

# A 3765233381 3383820361
# R 3383820361 3383820361

# S 2976589857 1620152520
# SA 3383820359 3765233381
#
#
# SYN(V,V)
# SYN+ACK(V,V)
# FIN(INV,V)
# ACK(V,V)
# ACK(V,V)
# RST(V,V)
# SYN(V,V)
# SYN+ACK(V,INV)


def scenario101():
    global seqVar
    sender = Sender(useTracking=True, isVerbose=1, networkPortMinimum=20000)
    random.seed(10)
    for i in range(1,400):
        seq = random.randrange(0,1000000)
        sender.sendReset()
        sender.sendInput("S", seq, 10000)
        sender.sendInput("F", seq+100, seqVar+1)
        sender.sendInput("A", seq+1, seqVar+1)
        sender.sendInput("S", seq-100, seqVar-100)
    #sender.sendInput("SA", seq+100, seqVar+1)

