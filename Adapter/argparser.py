__author__ = 'paul'
try:
    import argparse
    has_argparse = True
except ImportError:
    print 'argparse is not available, will use command line interface only'
    has_argparse = False
import sys
from ConfigParser import RawConfigParser
import args

class ArgumentParser:
    
    configValues = None
    # reads config values for arg parser only once
    def getConfigValues(self):
        if self.configValues is None:
            self.configValues = self.parseCmdArguments(sys.argv[1:], args.configArguments, fillWithDefault=True)
        return self.configValues
    
    # fetches the whole list of arguments
    def getArguments(self):
        arguments = []
        arguments.extend(args.senderArguments)
        arguments.extend(args.configArguments)
        arguments.extend(args.adapterArguments)
        return arguments
    
    # parses the settable arguments from command line, then from config file if option is given
    def parseArguments(self, settableArguments, configSection=None):
        parsedValues = {}
        configOptions = self.getConfigValues()
        # if reading from config file is enabled, stamp argument values read from config file to parsedValuesMap
        if configOptions["useConfig"] == True and configSection is not None:
            global has_argparse
            if has_argparse == False:
                print "cannot use the configuration parser because the \"argparse\" module couldn't be located"
                exit()
            configValues = self.parseConfigArguments(configOptions["configFile"], configSection, settableArguments, fillWithDefault=True)
            parsedValues.update(configValues)
            
        # stamp cmd  values read from cmd line to map (they will overwrite options set via config)
        if configOptions["useConfig"] == True:
            cmdValues = self.parseCmdArguments(sys.argv[1:], settableArguments, fillWithDefault=False)
        else:
            cmdValues = self.parseCmdArguments(sys.argv[1:], settableArguments, fillWithDefault=True)
        parsedValues.update(cmdValues)
        return parsedValues
        

    # parses arguments received from the command line
    # note networkPortMinimum and Maximum are only used in case port switching reset method is used
    def parseCmdArguments(self,cmdOptions, settableArguments, fillWithDefault=False):
        parser = argparse.ArgumentParser(prog="TCP Learner Adapter", description="Tool that transforms abstract messages"
        "received via a localCommunication into valid tcp/ip packets, sends them over the network, retrieves responses"
        "and transforms them back to abstract messages")
        for argument in settableArguments:
            if argument.type is None:
                parser.add_argument("-"+argument.definition, "--"+argument.fullDefinition, action="store_const", const=True, default=False, help=argument.description)
            else:
                if fillWithDefault == True:
                    parser.add_argument("-"+argument.definition, "--"+argument.fullDefinition, type=argument.type, default = argument.default, help=argument.description)
                else: 
                    parser.add_argument("-"+argument.definition, "--"+argument.fullDefinition, type=argument.type, help=argument.description)
        ns, _ = parser.parse_known_args(cmdOptions)
        reducedValues = dict((k, v) for k, v in vars(ns).iteritems() if v is not None) # build dict from namespace without None values
        return reducedValues

    # parses arguments received via a configuration file using the argparse module (see https://docs.python.org/2.7/library/argparse.html)
    def parseConfigArguments(self, configFile, configSection, settableArguments, fillWithDefault=False):
        values = {}
        config = RawConfigParser(defaults=values, allow_no_value=True)
        config.read(configFile)
        for argument in settableArguments:
            definition = None
            if config.has_option(configSection, argument.fullDefinition):
                definition = argument.fullDefinition
            elif config.has_option(configSection, argument.definition):
                definition = argument.definition
            if definition is not None:
                if argument.type is int:
                    values.update({argument.fullDefinition : config.getint(configSection,definition)})
                elif argument.type is bool:
                    values.update({argument.fullDefinition : config.getboolean(configSection,definition)})
                elif argument.type is float:
                    values.update({argument.fullDefinition : config.getfloat(configSection,definition)})
                else:
                    values.update({argument.fullDefinition : config.get(configSection,definition)[1:-1]})
            elif fillWithDefault==True:
                values.update({argument.fullDefinition : argument.default})
        return values

    # receives a list of arguments and a map of argument definitions to values. Selects only definition - value pairs that
    # are relevant to the set of given arguments 
    # no longer needed
    def getValueMapForArguments(self, arguments, parsedValues):
        valueMap = {}
        argumentDefinitions = map(lambda x: {x.fullDefinition: parsedValues.get(x.fullDefinition, x.default)}, arguments)
        for argumentDefinition in argumentDefinitions:
            valueMap.update(argumentDefinition)
        return valueMap
