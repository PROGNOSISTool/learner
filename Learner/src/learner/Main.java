package learner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import sutInterface.SimpleSutWrapper;
import sutInterface.SutInfo;
import sutInterface.quic.LearnResult;
import sutInterface.quic.SutInterfaceBuilder;
import util.Chmod;
import util.Container;
import util.FileManager;
import util.Log;
import util.ObservationTree;
import util.SoundUtils;
import util.Tuple2;
import util.exceptions.BugException;
import util.exceptions.CacheInconsistencyException;
import util.exceptions.CorruptedLearningException;
import util.learnlib.DotDo;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.algorithms.packs.ObservationPack;
import de.ls5.jlearn.equivalenceoracles.RandomWalkEquivalenceOracle;
import de.ls5.jlearn.exceptions.ObservationConflictException;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Learner;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.logging.LearnLog;
import de.ls5.jlearn.logging.LogLevel;
import de.ls5.jlearn.logging.PrintStreamLoggingAppender;
import de.ls5.jlearn.shared.WordImpl;
import de.ls5.jlearn.util.DotUtil;

public class Main {
	public static final String CACHE_FILE = "cache.ser";

	private static File sutConfigFile = null;
	public static LearningParams learningParams;
	private static long timeSnap = System.currentTimeMillis();;
	public static final String outputDir = "output" + File.separator + timeSnap;
	private static File outputFolder = null;
	public static PrintStream learnOut;
	public static PrintStream absTraceOut, absAndConcTraceOut;
	public static PrintStream stdOut = System.out;
	public static PrintStream errOut = System.err;
	public static PrintStream statsOut;
	private static boolean done;
	public static Config config;
	private static File sutInterfaceFile;
	private static ObservationTree tree;
	private static SimpleSutWrapper sutWrapper;
	private static Container<Integer>
				nrMembershipQueries = new Container<>(),
				nrEquivalenceQueries = new Container<>(),
				nrUniqueEquivalenceQueries = new Container<>(),
				nrResets = new Container<>();
	private static IOEquivalenceOracle yanOracle;
	private static IOEquivalenceOracle yanOracle2;

	private static List<Runnable> shutdownHooks = new ArrayList<>();

	public static void main(String[] args) throws LearningException, IOException, Exception {
		try{
			// wrapper around main to return a useful error code upon nondeterminism
			runLearner(args);
		} catch (CorruptedLearningException e) {
			System.exit(42);
		}
	}

	public static void runLearner(String[] args) throws LearningException, IOException, Exception {
		nrMembershipQueries.value = nrEquivalenceQueries.value = nrResets.value = nrUniqueEquivalenceQueries.value = 0;

		System.out.println("Reading program arguments");
		handleArgs(args);

		System.out.println("Setting up output directory...");
		setupOutput(outputDir);

		System.out.println("Reading config...");
		Config config = createConfig();
		Main.config = config;

		System.out.println("Creating SUT interface...");
		SutInterface sutInterface = createSutInterface(config);

		System.out.println("Reading TCP parameters...");
		SUTParams tcp = readConfig(config, sutInterface);

		Log.setLogLevel(tcp.logLevel);

		// first is the membership, second is the equivalence oracle
		System.out.println("Building oracles...");
		Tuple2<Oracle,Oracle> tcpOracles = buildOraclesFromConfig(tcp);

		Learner learner;

		LearnResult learnResult;

		System.out.println("Building equivalence oracle...");
		de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle = buildEquivalenceOracle(learningParams, tcpOracles.tuple1);
		SingleTransitionReducer ceReducer = new SingleTransitionReducer(tcpOracles.tuple1);

		learner = new ObservationPack();
		//learner = new Angluin();
		learner.setOracle(tcpOracles.tuple0);

		learner.setAlphabet(SutInfo.generateInputAlphabet());
		SutInfo.generateOutputAlphabet();

		System.out.println("Starting learner...");
		learnResult = learn(learner, eqOracle, ceReducer);


		// final output to out.txt
		absTraceOut.println("Seed: " + learningParams.seed);
		errOut.println("Seed: " + learningParams.seed);
		absTraceOut.println("Done.");
		errOut.println("Successful run.");

		// output needed for equivalence checking
		// - learnresult.dot : learned state machine
		// - output.json : abstraction,concrete alphabet, start state
		State startState = learnResult.learnedModel.getStart();

		statsOut
				.println("Total states in learned abstract Mealy machine: "
						+ learnResult.learnedModel.getAllStates().size());

		Statistics.getStats().printStats(statsOut);

		// output learned model with start state highlighted to dot file :
		// notes:
		// - make start state the only highlighted state in dot file
		// - learnlib makes highlighted state by setting attribute color='red'
		// on state

		LinkedList<State> highlights = new LinkedList<State>();
		highlights.add(startState);

		writeOutputFiles(learnResult, highlights);

		errOut.println("Learner Finished!");

		// bips to notify that learning is done :)
		SoundUtils.success();
	}

	private static void copyInputsToOutputFolder() {
		File inputFolder = sutConfigFile.getParentFile();
		if (!inputFolder.getName().equalsIgnoreCase("input")) {
		    Log.err("Could not find input folder \"input\", so not copying ");
		    return;
		}
		Path srcInputPath = inputFolder.toPath();
		Path dstInputPath = outputFolder.toPath().resolve(inputFolder.getName()); // or resolve("input")
		//Path srcTcpPath = Paths.get(System.getProperty("user.dir")).resolve("Learner").resolve("src").resolve("sutInterface").resolve("tcp");
		//Path dstTcpPath = outputFolder.toPath().resolve("tcp");
		try {
			FileManager.copyFromTo(srcInputPath, dstInputPath);
		//	FileManager.copyFromTo(srcTcpPath, dstTcpPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void writeOutputFiles(LearnResult learnResult,
			LinkedList<State> highlights) {
		// output learned state machine as dot and pdf file :
		//File outputFolder = new File(outputDir + File.separator + learnResult.startTime);
		//outputFolder.mkdirs();
		File dotFile = new File(outputFolder.getAbsolutePath() + File.separator + "learnresult.dot");
		File pdfFile = new File(outputFolder.getAbsolutePath() + File.separator + "learnresult.pdf");

		try (BufferedWriter out = new BufferedWriter(new FileWriter(dotFile))) {
			DotUtil.writeDot(learnResult.learnedModel, out, learnResult.learnedModel.getAlphabet()
					.size(), highlights, "");
			// write pdf
			DotUtil.invokeDot(dotFile, "pdf", pdfFile);
		} catch (IOException e1) {
			System.err.println("could not write to dot file");
		}
		try {
			Chmod.set(7, 7, 5, true, outputFolder.getAbsolutePath());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public static void setupOutput(final String outputDir) throws FileNotFoundException {
		outputFolder = new File(outputDir);
		outputFolder.mkdirs();
		absTraceOut = new PrintStream(
				new FileOutputStream(outputDir + File.separator + "tcpLog.txt", false));
		absAndConcTraceOut = new PrintStream(
						new FileOutputStream(outputDir + File.separator + "tcpTrace.txt", false));
		absAndConcTraceOut.println("copy this to obtain the regex describing any text between two inputs of a trace:\n[^\\r\\n]*[\\r\\n][^\\r\\n]*[\\r\\n][^\\r\\n]*[\\r\\n][^\\r\\n]*[\\r\\n]\n\n");
		learnOut = new PrintStream(
				new FileOutputStream(outputDir + File.separator + "learnLog.txt", false));
	    errOut = new PrintStream(
	                new FileOutputStream(outputDir + File.separator + "err.txt", false));
	    Log.setErrorPrintStream(errOut);

		statsOut = new PrintStream(
				new FileOutputStream(outputDir + File.separator + "statistics.txt", false));
		registerShutdownHook(new Runnable() {
			@Override
			public void run() {
				System.err.println("Running shutdown hook");
				copyInputsToOutputFolder();
				writeCacheTree(tree, true);
				Statistics.getStats().printStats(statsOut);
				Statistics.getStats().printStats(System.out);
				statsOut.flush();

				closeOutputStreams();
				//InitCacheManager mgr = new InitCacheManager();
				//mgr.dump(outputDir + File.separator +  "cache.txt");
				if (done == false) {
					SoundUtils.failure();
				}
			}
		});
	}

	private static LearnResult learn(Learner learner,
			de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle, SingleTransitionReducer ceReducer)
			throws LearningException, ObservationConflictException, IOException {
		LearnResult learnResult = new LearnResult();
		Statistics stats = Statistics.getStats();
		stats.startTime = System.currentTimeMillis();
		long starttmp = stats.startTime;
		int hypCounter = 1;
		long endtmp;
		done = false;

		Log.fatal("Start Learning");
		absTraceOut.println("starting learning\n");
		//try {
			while (!done) {
				absTraceOut.println("		RUN NUMBER: " + ++stats.runs);
				absTraceOut.println("");
				absTraceOut.flush();
				errOut.flush();

				try {
					// execute membership queries
					learner.learn();
					absTraceOut.flush();
					errOut.flush();
					absTraceOut.println("done learning");
					endtmp = System.currentTimeMillis();
					statsOut
							.println("Running time of membership queries: "
									+ (endtmp - starttmp) + "ms.");
					stats.totalMemQueries = nrMembershipQueries.value;
					stats.totalTimeMemQueries += endtmp - starttmp;
					starttmp = System.currentTimeMillis();
					absTraceOut.flush();

					// stable hypothesis after membership queries
					Automaton hyp = learner.getResult();
					String hypFileName = outputDir + File.separator + "tmp-learnresult"
							+ hypCounter + ".dot";
					String hypPdfFileName = outputDir + File.separator + "tmp-learnresult"
							+ hypCounter + ".pdf";

					File hypPDF = new File(hypPdfFileName);
					DotDo.writeDotFile(hyp, hypFileName );
					DotUtil.invokeDot(hypFileName, "pdf", hypPDF);

					absTraceOut.println("starting equivalence query");
					absTraceOut.flush();
					errOut.flush();
					// search for counterexample
					EquivalenceOracleOutput o = null;
					o = eqOracle
						.findCounterExample(hyp);
					int nrHypTests = getNrHypTests();
					stats.addNrHypothesisEquivalenceQueries(nrHypTests);

					stats.totalEquivQueries = nrEquivalenceQueries.value;
					stats.totalUniqueEquivQueries = nrUniqueEquivalenceQueries.value;
					absTraceOut.flush();
					errOut.flush();
					absTraceOut.println("done equivalence query");
					endtmp = System.currentTimeMillis();
					stats.totalTimeEquivQueries += endtmp - starttmp;
					starttmp = System.currentTimeMillis();

					// no counter example -> learning is done
					if (o == null) {
						done = true;
						continue;
					}
					o = ceReducer.reducedCounterexample(o, hyp);

					logCounterExampleAnalysis(hyp, hypCounter, o);
					hypCounter ++;
					absTraceOut.println("Sending CE to LearnLib.");
					absTraceOut.println("Counter Example: "
							+ o.getCounterExample().toString());
					absTraceOut.flush();
					errOut.flush();
					// return counter example to the learner, so that it can use
					// it to generate new membership queries
					learner.addCounterExample(o.getCounterExample(),
							o.getOracleOutput());
					absTraceOut.flush();
					errOut.flush();
				} catch (CacheInconsistencyException e) {
					stats.totalEquivQueries = nrEquivalenceQueries.value;
					stats.totalUniqueEquivQueries = nrUniqueEquivalenceQueries.value;
					stats.totalMemQueries = nrMembershipQueries.value;
					throw e;
				}
			}
		stats.endTime = System.currentTimeMillis();
		learnResult.learnedModel = learner.getResult();
		return learnResult;
	}

	private static int getNrHypTests() {
	    int nrTests = 0;
	    if (yanOracle != null) {
            nrTests += yanOracle.getNrHypthesisTests();
            yanOracle.clearNrHypTests();
        }
        if (yanOracle2 != null) {
            nrTests += yanOracle2.getNrHypthesisTests();
            yanOracle2.clearNrHypTests();
        }
        return nrTests;
    }

    private static void logCounterExampleAnalysis(Automaton hyp, int hypCounter, EquivalenceOracleOutput o) throws IOException {
		PrintStream out = new PrintStream( new FileOutputStream(outputDir + File.separator +"cexanalysis.txt", true));
		Word ceInputWord = o.getCounterExample();
		Word oracleOutputWord = o.getOracleOutput();
		List<Symbol> ceInputSymbols = ceInputWord.getSymbolList();
		List<Symbol> sutOutput = oracleOutputWord.getSymbolList();
		List<Symbol> inputSymbols = new ArrayList<Symbol>();
		List<Symbol> hypOutput = hyp.getTraceOutput(ceInputWord).getSymbolList();
		out.print("\n Counterexample for hyp"+hypCounter +"\n");

		for (int i = 0; i < ceInputSymbols.size(); i++) {
			inputSymbols.add(ceInputSymbols.get(i));
			Word inputWord = new WordImpl((Symbol[]) inputSymbols.toArray(new Symbol[inputSymbols.size()]));
			out.println(ceInputSymbols.get(i));
			out.println("!" +hypOutput.get(i) + " s" + hyp.getTraceState(inputWord, i+1).getId());

			if (! hypOutput.get(i).equals( sutOutput.get(i))) {
				out.println("#!" +sutOutput.get(i));
				break;
			}
		}
		out.close();
	}

	private static void closeOutputStreams() {
		statsOut.close();
		absTraceOut.close();
		absAndConcTraceOut.close();
		learnOut.close();
		errOut.close();
	}

	// I hacked in the additional yannakakis command
	private static de.ls5.jlearn.interfaces.EquivalenceOracle buildEquivalenceOracle(LearningParams learningParams, Oracle queryOracle) {
		List<de.ls5.jlearn.interfaces.EquivalenceOracle> eqOracles = new ArrayList<de.ls5.jlearn.interfaces.EquivalenceOracle>();
		if (learningParams.yanCommand == null) {
			Random random = new Random(learningParams.seed);
			RandomWalkEquivalenceOracle randEqOracle = new RandomWalkEquivalenceOracle(learningParams.maxNumTraces,
					learningParams.minTraceLength, learningParams.maxTraceLength);
			randEqOracle.setOracle(queryOracle);
			randEqOracle.setRandom(random);
			eqOracles.add(randEqOracle);
		}
		if (learningParams.testTraces != null && !learningParams.testTraces.isEmpty()) {
            WordCheckingEquivalenceOracle wordEqOracle = new WordCheckingEquivalenceOracle(queryOracle, learningParams.testTraces);
            eqOracles.add(wordEqOracle);
        }

		if (learningParams.yanCommand != null) {
		    yanOracle = new IOEquivalenceOracle(queryOracle, learningParams.maxNumTraces, learningParams.yanCommand, nrUniqueEquivalenceQueries);
			eqOracles.add(yanOracle);
		}
	    if (learningParams.yanCommand2 != null) {
	        yanOracle2 = new IOEquivalenceOracle(queryOracle, Integer.MAX_VALUE, learningParams.yanCommand2, nrUniqueEquivalenceQueries);
	        eqOracles.add(yanOracle2);
	    }
	    de.ls5.jlearn.interfaces.EquivalenceOracle eqOracle = null;
	    if (eqOracles.isEmpty()) {
	        throw new BugException("No equivalence oracle could be defined");
	    } else if (eqOracles.size() == 1) {
	        eqOracle = eqOracles.get(0);
	    } else {
	        eqOracle = new CompositeEquivalenceOracle(eqOracles.toArray(new de.ls5.jlearn.interfaces.EquivalenceOracle [eqOracles.size()]));
	    }
		return eqOracle;
	}

	private static Tuple2<Oracle, Oracle> buildOraclesFromConfig(SUTParams quic) {
		System.out.println("Building SUT wrapper...");
		sutWrapper = new SimpleSutWrapper(quic.sutIP, quic.sutPort);

		System.out.println("Building cache tree...");
		tree = readCacheTree(CACHE_FILE);
		if (tree == null) {
			tree = new ObservationTree();
		}

		int minAttempts = quic.runsPerQuery, maxAttempts = 100;
		double probFraction = (double) quic.confidence / 100;

		SutInterfaceBuilder builder = new SutInterfaceBuilder();
		Oracle eqOracleRunner = builder
				.sutWrapper(sutWrapper)
				.equivalenceOracle(nrEquivalenceQueries, nrUniqueEquivalenceQueries)
				//.runMultipleTimes(2)
				.probablisticOracle(minAttempts, probFraction, maxAttempts)
				.logger()
				.cacheReaderWriter(tree)
				//.probablisticNonDeterminismValidator(10, 0.8, tree)
				.askOnNonDeterminsm(tree)
				.resetCounter(nrResets)
				.uniqueQueryCounter(nrUniqueEquivalenceQueries)
				.queryCounter(nrEquivalenceQueries)
				.learnerInterface();
		Oracle memOracleRunner = builder
				.sutWrapper(sutWrapper)
				.membershipOracle(nrMembershipQueries)
				//.runMultipleTimes(2)
				.probablisticOracle(minAttempts, probFraction, maxAttempts)
				.logger()
				.cacheReaderWriter(tree)
				//.probablisticNonDeterminismValidator(10, 0.8, tree)
				.askOnNonDeterminsm(tree)
				.resetCounter(nrResets)
				.queryCounter(nrMembershipQueries)
				.learnerInterface();

		return new Tuple2<Oracle,Oracle>(memOracleRunner, eqOracleRunner);
	}

	public static SUTParams readConfig(Config config, SutInterface sutInterface) {
		// read/disp config params for learner
		learningParams = config.learningParams;
		learningParams.printParams(absTraceOut);

		// read sut interface information
		SutInfo.setMinValue(learningParams.minValue);
		SutInfo.setMaxValue(learningParams.maxValue);

		SutInfo.setInputSignatures(sutInterface.inputInterfaces);
		SutInfo.setOutputSignatures(sutInterface.outputInterfaces);

		LearnLog.addAppender(new PrintStreamLoggingAppender(LogLevel.DEBUG,
				learnOut));

		// read/disp SUT config
		SUTParams sut = config.sutParams;
		sut.printParams(absTraceOut);
		return sut;
	}

	public static SutInterface createSutInterface(Config config)
			throws FileNotFoundException {
		sutInterfaceFile = new File(sutConfigFile
				.getParentFile().getAbsolutePath()
				+ File.separator
				+ config.learningParams.sutInterface);
		InputStream sutInterfaceInput = new FileInputStream(sutInterfaceFile);
		Yaml yaml = new Yaml(new Constructor(SutInterface.class));
		SutInterface sutInterface = (SutInterface) yaml.load(sutInterfaceInput);
		return sutInterface;
	}

	public static Config createConfig() throws FileNotFoundException {
		InputStream configInput = new FileInputStream(sutConfigFile);
		Yaml yaml = new Yaml(new Constructor(Config.class));
		Config config = (Config) yaml.load(configInput);
		return config;
	}

	public static void handleArgs(String[] args) {
		if (args.length == 0) {
			errOut.println("Use: java Main config_file");
			System.exit(-1);
		}
		sutConfigFile = new File(args[0]);
		if (sutConfigFile.exists() == false) {
			errOut.println("The sut config file " + args[0]
					+ " does not exist");
			System.exit(-1);
		}
		sutConfigFile = sutConfigFile.getAbsoluteFile();
	}

	public static void printUsage() {
		System.out
				.println(" config_file     - .yaml config file describing the sut/learning.");
	}

	public static int cachedTreeNum = 0;

	public static ObservationTree getTree() {
	    return tree;
	}

	public static void writeCacheTree(ObservationTree tree, boolean isFinal) {

	    String cachePath = CACHE_FILE;
	    String indexedCachePath = cachedTreeNum + CACHE_FILE;
		writeCacheTree(tree, isFinal?cachePath:indexedCachePath);
		if (outputFolder != null) {
    		cachePath = outputDir + File.separator + cachePath;
    		indexedCachePath = outputDir + File.separator + indexedCachePath;
    		writeCacheTree(tree, isFinal?cachePath:indexedCachePath);
		}
	}

	public static void writeCacheTree(ObservationTree tree, String fileName) {
		if (tree == null) {
			System.err.println("Could not write uninitialized observation tree");
			return;
		}
		try (
				OutputStream file = new FileOutputStream(fileName);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);
				) {
			output.writeObject(new Tuple2<>(tree, Statistics.getStats().totalUniqueEquivQueries));
			output.close();
		}
		catch (IOException ex){
			System.err.println("Could not write observation tree");
		}
		Main.cachedTreeNum += 1;
	}

	public static ObservationTree readCacheTree(String fileName) {
		try(
				InputStream file = new FileInputStream(fileName);
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream (buffer);
				) {
			@SuppressWarnings("unchecked")
			Tuple2<ObservationTree, Integer> deserialised = (Tuple2<ObservationTree, Integer>) input.readObject();
			Statistics.getStats().totalUniqueEquivQueries = deserialised.tuple1;
			return deserialised.tuple0;
		}
		catch(ClassNotFoundException ex) {
			System.err.println("Cache file corrupt");
			return null;
		}
		catch(IOException ex) {
			System.err.println("Could not read cache file");
			return null;
		}
	}

	public static void registerShutdownHook(Runnable r) {
		Runtime.getRuntime().addShutdownHook(new Thread(r));
		shutdownHooks.add(r);
	}
}


