package learner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import net.automatalib.automata.transducers.MealyMachine;
import util.Log;
import util.learnlib.DotDo;

public class YannakakisWrapper<O> implements TestGenerator {
    private final ProcessBuilder pb;
    private Process process;
    private Writer processInput;
    private BufferedReader processOutput;
    private StreamGobbler errorGobbler;
    private MealyMachine<?, String, ?, O> hyp;
    private Collection<? extends String> inputs;

    public YannakakisWrapper(MealyMachine<?, String, ?, O> inputEnabledHypothesis,
			Collection<? extends String> inputs,
            String yannakakisCmd) {
        this.hyp = inputEnabledHypothesis;
        this.inputs = inputs;
        this.pb = new ProcessBuilder(yannakakisCmd.split("\\s"));
        Main.registerShutdownHook(() -> {
			if (!isClosed()) {
				closeAll();
				Log.err("Shutting down process");
			}
		});
    }

    public BufferedReader out() {
        return processOutput;
    }

    public void terminate() {
        close();
    }

    public void close() {
        closeAll();
    }

    public void initialize() {
        try {
            setupProcess();
            sendHypToProcess();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHypToProcess() throws IOException {
        // the hyp is transformed to a dot state machine string
        StringWriter sw = new StringWriter();
        DotDo.write(hyp, inputs, sw);
        String dotString = sw.toString();

        // the dot string is modified to correspond with the dot version used by
        // the Yannakakis tool
        dotString = dotString
                .replaceAll(
                        "<<table border=\"0\" cellpadding=\"1\" cellspacing=\"0\"><tr><td>",
                        "\"");
        dotString = dotString.replaceAll("</td><td>", " ");
        dotString = dotString.replaceAll("</td></tr></table>>", "\"");

        // we input the dot string to the Yannakakis tool and flush
        processInput.append(dotString);
        processInput.flush();
    }

    public List<String> nextTest() throws IOException {
        List<String> nextTest = null;
        String line = out().readLine();
        if ( line != null) {
            nextTest = getNextTestFromLine(line);
        }
        return nextTest;
    }

    private List<String> getNextTestFromLine(String line) {
        ArrayList<String> testQuery = new ArrayList<String>();

        Scanner s = new Scanner(line);
        while (s.hasNext()) {
            testQuery.add(s.next());
        }
        s.close();

        return testQuery;
    }

    /**
     * Strips the tags produces by learnlib 2 so that Yannakakis can use it
     */
    public String stripTagsFromDot(String dotWithTags) {
        return dotWithTags
                .replaceAll(
                        "<<table border=\"0\" cellpadding=\"1\" cellspacing=\"0\"><tr><td>",
                        "\"").replaceAll("</td><td>", " ")
                .replaceAll("</td></tr></table>>", "\"");
    }

    /**
     * A small class to print all stuff to stderr. Useful as I do not want
     * stderr and stdout of the external program to be merged, but still want to
     * redirect stderr to java's stderr.
     */
	static class StreamGobbler extends Thread {
        private final InputStream stream;
        private final String prefix;

        StreamGobbler(InputStream stream, String prefix) {
            this.stream = stream;
            this.prefix = prefix;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null)
                    System.err.println(prefix + "> " + line);
            } catch (IOException e) {
                // It's fine if this thread crashes, nothing depends on it
                e.printStackTrace();
            }
        }
    }

    private boolean isClosed() {
        return process == null;
    }

    /**
     * Starts the process and creates buffered/whatnot streams for stdin stderr
     * or the external program
     *
     * @throws IOException
     *             if the process could not be started (see ProcessBuilder.start
     *             for details).
     */
    private void setupProcess() throws IOException {
        process = pb.start();
        processInput = new OutputStreamWriter(process.getOutputStream());
        processOutput = new BufferedReader(new InputStreamReader(
                process.getInputStream()));
        errorGobbler = new StreamGobbler(process.getErrorStream(),
				"ERROR> main");
        errorGobbler.start();
    }

    /**
     * I thought this might be a good idea, but I'm not a native Java speaker,
     * so maybe it's not needed.
     */
    private void closeAll() {
        // Since we're closing, I think it's ok to continue in case of an
        // exception
        try {
            processInput.close();
            processOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            errorGobbler.join(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        process.destroy();
        process = null;
        processInput = null;
        processOutput = null;
        errorGobbler = null;
    }
}
