package util.learnlib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.ExceptionAdapter;

import de.ls5.jlearn.interfaces.Alphabet;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.shared.AlphabetImpl;
import de.ls5.jlearn.shared.AutomatonImpl;

public class TCPDot {
	private static final String LABEL = "label=<<table border=\"0\" "
			+ "cellpadding=\"1\" cellspacing=\"0\"><tr><td>";
	private static final String MID = "</td><td>/</td><td>";

	private static class TempState {
		public State state;
		public final Map<String, TempState> next = new HashMap<String, TempState>();
		public final Map<String, String> out = new HashMap<String, String>();
	}

	public static Automaton readFile(String filename) throws IOException {
		Alphabet alphabet = new AlphabetImpl();
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		Map<String, TempState> states = new HashMap<String, TempState>();
		while (line != null) {
			if (line.contains("->")) {
				int nextSpace = line.indexOf(' ');
				String from = line.substring(0, nextSpace);
				int nextS = line.indexOf('s', nextSpace);
				int openBraket = line.indexOf('[');
				String to = line.substring(nextS, openBraket);
				int beginMid = line.indexOf('<', openBraket + LABEL.length());
				String input = line.substring(openBraket + LABEL.length() + 1,
						beginMid);
				int endOutput = line.indexOf('<', beginMid + MID.length());
				String output = line.substring(beginMid + MID.length(),
						endOutput);
				Symbol inputSym = SymbolCache.getSymbol(input);
				alphabet.addSymbol(inputSym);
				TempState fromState = states.get(from);
				if (fromState == null) {
					fromState = new TempState();
					states.put(from.intern(), fromState);
				}
				TempState toState = states.get(to);
				if (toState == null) {
					toState = new TempState();
					states.put(to.intern(), toState);
				}
				fromState.next.put(input.intern(), toState);
				fromState.out.put(input.intern(), output.intern());
			}
			line = reader.readLine();
		}
		Automaton result = new AutomatonImpl(alphabet);
		states.get("s0").state = result.getStart();
		for (TempState state : states.values()) {
			if (state.state == null) {
				State temp = result.addNewState();
				state.state = temp;
			}
		}
		for (TempState state : states.values()) {
			for (String input : state.next.keySet()) {
				Symbol inputSym = SymbolCache.getSymbol(input);
				Symbol outputSym = SymbolCache.getSymbol(state.out.get(input));
				state.state.setTransition(inputSym,
						state.next.get(input).state, outputSym);
			}

		}
		if (!result.isWellDefined()) {
			System.out.println("Automata no well defined!");
		}
		reader.close();
		return result;
	}
	
    // policy : convert into method throwing unchecked exception 
	public static Automaton readDotFile(String filepath)  {
		try {
			return readFile(filepath);  
		} catch (IOException ex) {
			throw new ExceptionAdapter(ex); 
		} 		
	}		
	
	/* write dot file in a deterministic sorted way :
	 *   - alphabetic order the input alphabet    
 	 *   - using this alphabetic ordered input alphabet do a breadthfirst search to all the states of the automaton
	 *   - store the states in this order
	 *   - for the same order of states store the transitions between the states
	 *   - multiple transitions between a pair of states are ordered by the input order 
	 *     from the alphabetic ordered input alphabet  
	 */  
	static public void writeFile(Automaton model,String filepath, List<State> highlights, String description, boolean doubleCircleStartState,Set<Symbol> hide) throws IOException {	    
		BufferedWriter outstream= new BufferedWriter(new FileWriter(filepath));
		write(model, outstream, highlights, description, doubleCircleStartState, hide );  
		outstream.close();
	}		
	
	//policy: 
	//  write dotfile with red double circeled start state 	
	public static void writeFile(Automaton automaton, String filepath) throws IOException {

	    
		// highlight (red) only startstate 
		LinkedList<State> highlights=new LinkedList<State>();
	    State startState=automaton.getStart();
	    highlights.add(startState);   // states which are colored red in dotfile

	    writeFile( automaton, filepath,  highlights ,"" , true, new HashSet() );		
	}

    // policy : convert into method throwing unchecked exception 
	static public void writeDotFile(Automaton model,String filepath ) {
		try {
			 writeFile(model, filepath);  
		} catch (IOException ex) {
			throw new ExceptionAdapter(ex); 
		} 		
	}

	/* write 
	 *   same as writeFile but then to Appendable instead of filepath 
	 * 
	 */
	public static void write(Automaton automaton, Appendable out, List<State> highlights, String description, boolean doubleCircleStartState,Set<Symbol> hide) {
		Map<State, String> tcpStateMap = AutomatonTCPStateMatcher.produceTCPStateMap(automaton);
		List<Symbol> inputs=automaton.getAlphabet().getSymbolList();
		java.util.Collections.sort(inputs);
		
		 List<State> states = util.learnlib.AutomatonUtils.getStatesInBFSOrder(automaton);
/*		
		for( Symbol input:  inputs ) {
			
			
		}
		*/
		
		//List<Symbol>	getSymbolList().sort();
		

	      HashMap<State,String> labels = new HashMap<State,String>();
	//      List states = automaton.getAllStates();
	      Set <State>  highlightsSet = new HashSet <State> ();
	      if (highlights != null) {
	        for (State s : highlights) {
	          highlightsSet.add(s);
	        }
	      }
     try {
			out.append("digraph G {\n");

	      if (description != null) {
	        out.append(new StringBuilder().append("label=\"").append(escapeDot(description)).append("\"").toString());
	      }

	   //   states = AutomatonUtil.getStatesInBFSOrder(a);

	      out.append("\n");
	      
	      // walk over states to print them with highlighter if set
	      // and also immmediate store a state's label  in 'labels' mapping ( state -> label )
	      int i = 0;
	      for (State s : states) {
	    	  String stateLabel;
	    	  if (tcpStateMap.containsKey(s)) {
	    		  stateLabel = tcpStateMap.get(s);
	    	  } else {
	    		  stateLabel = new StringBuilder().append("s").append(i).toString();
	    	  }
	    	  
	        if (highlightsSet.contains(s))
	          out.append(new StringBuilder().append(stateLabel).append(" [color=\"red\"]\n").toString());
	        else {
	          out.append(new StringBuilder().append(stateLabel).append("\n").toString());
	        }
	        labels.put(s, new StringBuilder().append(stateLabel).toString());
	        i++;
	      }
	      
	      // for each state
	      // - print state 
	      // - print its transitions
	      for (State s : states) {
	        if (s != null) {
	          String label = (String)labels.get(s);
	          out.append(new StringBuilder().append(label).append(" [label=\"").append(escapeDot(label)).append("\"];\n").toString());

//	          for (Symbol letter : s.getInputSymbols()) {
	          for (Symbol letter : inputs) {
	            //if (automaton.getAlphabet().getIndexForSymbol(letter) < realsigma) {
	              String destlabel = (String)labels.get(s.getTransitionState(letter));

	              if (hide.contains(s.getTransitionOutput(letter)))
	              {
	                continue;
	              }
	              if (destlabel != null) {
	                out.append(new StringBuilder().append(label).append(" -> ").append(destlabel).append("[label=<<table border=\"0\" cellpadding=\"1\" cellspacing=\"0\"><tr><td>").append(getHTMLString(letter)).append("</td><td>/</td><td>").append(getHTMLString(s.getTransitionOutput(letter))).append("</td></tr></table>>]\n").toString());
	              }
	           // }
	          }
	          
	        }
	      }
	      out.append("}\n"); 

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	      
	}	

//-----------------------------------------------------------------------------------------------------------------------------
//  helpers for new writedot
//-----------------------------------------------------------------------------------------------------------------------------
	  private static String escapeDot(String s)
	  {
	    return s.replace("\"", "\\\"");
	  }

	private static String getHTMLString(Symbol sym) {
		    return escapeDot(sym.toString());
    }
}
