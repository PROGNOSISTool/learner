class Argument:
    def __init__(self, definition, fullDefinition, argumentType, defaultValue, description):
        self.definition = definition
        self.fullDefinition = fullDefinition
        self.description = description
        self.default = defaultValue
        self.type = argumentType

# list of arguments for each component in the setup
# apart from config, each list has a corresponding section in the config file
# senderArguments has the section [sender], runnerArguments has the section [runner]....

adapterArguments = [
    Argument("p","socketPort", int, 18200, "Listening adapter port which the learner connects to"),
    Argument("ip","socketIP", str, 'localhost', "Listening adapter IP which the learner connects to"),
    Argument("con","continuous", bool, True, "Listening adapter handles clients continuously"\
             " and doesn't close its server socket after first client is handled")
]

configArguments = [
    Argument("c","useConfig", bool, True, "Sets whether the tool will read sender args from a configuration file"),
  #  Argument("csec","configSection", str, "tcp", "The section in the configuration file."),
    Argument("cfile","configFile", str, "input/config.cfg", "The configuration file used. Preferably left as the default value.")]

senderArguments = [
    Argument("ni","networkInterface", str, "eth0","The net interface through which the client communicates"),
    Argument("sp","senderPort", int, 20000,"Active adapter port "),
    Argument("spmin","senderPortMinimum", int, 20000, "Set the minimum boundary and starting number for "
                                                   "the network port"),
    Argument("spmax","senderPortMaximum", int, 20000, "Set the maximum boundary after which it reverts back to "
                                                   "networkPortMinimum"),
    Argument("v","isVerbose", bool, True, "If true then more text will be displayed"),
    Argument("pnf","portNumberFile", str, "sn.txt", "File with the port number"),
    Argument("ut","useTracking", bool, True, "If set, then the tracker is used along with the Scapy tool"),
    Argument("wt","waitTime",float, 0.06, "Sets the time the adapter waits for a response before concluding a timeout"),
    Argument("sip","serverIP", str, "192.168.56.1", "The TCP server"),
    Argument("sport","serverPort", int, 20000, "The server port"),
    Argument("smac","serverMAC", str, None, "The server MAC address"),
    Argument("rst","resetMechanism", int, 0, "0 selects reset by sending a valid RST and not changing the port,"
                                               "1 selects reset by changing the port (+1)"
                                                "2 selects a hybrid, where a valid RST is sent and the port is changed"),
    Argument("sa","sendActions", bool, False, "If true then the sender will be augmented with send action capability, so that "\
                                                "it can also send higher level actions to an command server adapter")]

runnerArguments = [
    Argument("r","runNum", int, 1,"The number of times the trace is run"),
    Argument("s","skipNum", int, 0,"The number of lines skipped after each abstract input is read"),
    Argument("j","jvmPath", str, "/usr/lib/jvm/jdk1.7.0_71/jre/lib/i386/server/libjvm.so" ,"Path to libjvm.so")]

actionSenderArguments = [
    Argument("cip","cmdIp", str, "192.168.56.1","The ip of the server adapter"),
    Argument("cport","cmdPort", int, 5000,"The port of the server adapter")]
