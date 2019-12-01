package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class RunCmd {

	/*
	 *  Function to automatically redirect output from a subprocess
	 *  to another output stream.
	 *
	 *   e.g. redirectIO(process.getInputStream(), System.out);
	 *
	 *   Note: process.getInputStream() is the output from the subprocess
	 *         which is input for current process!!
	 */
	
	public  static void redirectIO(final InputStream in,final OutputStream out)  {
	//	http://stackoverflow.com/questions/60302/starting-a-process-with-inherited-stdin-stdout-stderr-in-java-6
		    new Thread(new Runnable() {
		        public void run() {
		            try {
		    		    while (true) {
		    			      int c = in.read();
		    			      if (c == -1) break;
		    			      out.write((char)c);
		    			}
		            } catch (IOException e) { // just exit
		    	    	System.err.println("Problem handling IO of running command : " );
		    	    	e.printStackTrace(); // to stderr stream
		            }
		        }
		    }).start();

    }


	
	/*
	 *  Run a command in a separate process and redirect all its output to the given PrintStream
	 *
	 *  eg. runCmd("python plot.py",System.out,true);
	 *  
	 *  runCmd("python plot.py",stdout,false);try {Thread.sleep(2000);} catch (Exception e) {};System.exit(-1);
     *  runCmd("python plot.py",stdout,true);System.exit(-1);
	 */
	public  static void runCmd(final String str,final PrintStream outstream, final boolean wait)  {
		Thread t=new Thread(new Runnable() {
	        public void run() {

			    try {
			    	Process process = Runtime.getRuntime().exec (str);
			    	redirectIO(process.getInputStream(), outstream);
			    	redirectIO(process.getErrorStream(), System.err);
			    	
			    	
			    	  process.waitFor();
			    	  outstream.flush();
			    	  System.err.flush();
				    	
					  if ( process.exitValue() != 0) {
						System.err.println("\nExit(" + process.exitValue() +  "): Problem  running command : " + str );
						System.exit(process.exitValue());
					  }
			        
			    } catch (IOException e) {
			    	System.err.println("Problem handling IO of running command : " + str );
			    	e.printStackTrace(); // to stderr stream
				} catch (Exception e) {
			    	System.err.println("Problem  running command : " + str );
			    	e.printStackTrace(); // to stderr stream			
				}
		    }});
		t.start();	
		if (wait) {
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.err.println("\nExit: Problem  running command : " + str );
				//e.printStackTrace();
			}
		}
    }
			
	
}
