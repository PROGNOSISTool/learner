config.cfg configures the Adapter, config.yaml the Learner.  
You have to adjust config.cfg to your setting, namely you have to
set:
	the port the adapter is listening at (localCommunicationPort)
	if the adapter should also listen for actions (sendActions)
	the IP and port of the SUT you're sending packets to (serverIP, serverPort)
	the IP and port of the TCP Adapter you're sending action strings to (cmdIP, cmdPort)
	the time the adapter waits for a response before signaling a timeout (waitTime)
	if the tracker is used (improved packet receipt) (useTracker)
	
	
In order to run using these setup files, from the project directory run:
sudo python Adapter/main.py -c -cfile Example/config.cfg -csec "tcp"
Within eclipse, run the Learner using:
Example/config.yaml 
as parameter.
