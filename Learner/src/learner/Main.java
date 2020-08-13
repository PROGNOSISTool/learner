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
import java.lang.reflect.Member;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.learnlib.algorithms.lstar.mealy.ClassicLStarMealy;
import de.learnlib.algorithms.lstar.mealy.ClassicLStarMealyBuilder;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.SUL;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.filter.cache.LearningCacheOracle;
import de.learnlib.oracle.equivalence.MealyRandomWordsEQOracle;
import de.learnlib.oracle.equivalence.RandomWordsEQOracle;
import de.learnlib.oracle.equivalence.mealy.RandomWalkEQOracle;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.util.mealy.MealyUtil;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import sutInterface.CacheReaderOracle;
import sutInterface.ProbabilisticOracle;
import sutInterface.SutInfo;
import sutInterface.quic.LearnResult;
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
import de.ls5.jlearn.exceptions.ObservationConflictException;
import de.ls5.jlearn.logging.LearnLog;
import de.ls5.jlearn.logging.LogLevel;
import de.ls5.jlearn.logging.PrintStreamLoggingAppender;
import de.ls5.jlearn.util.DotUtil;

public class Main {
	public static final String CACHE_FILE = "cache.ser";

	private static File sutConfigFile = null;
	public static LearningParams learningParams;
	private static final long timeSnap = System.currentTimeMillis();;
	public static final String outputDir = "output" + File.separator + timeSnap;
	private static File outputFolder = null;
	public static PrintStream learnOut;
	public static PrintStream absTraceOut, absAndConcTraceOut;
	public static PrintStream stdOut = System.out;
	public static PrintStream errOut = System.err;
	public static PrintStream statsOut;
	private static boolean done;
	public static Config config;
	private static ObservationTree tree;
	private static Alphabet<String> alphabet;
	private static final Container<Integer>
				nrMembershipQueries = new Container<>();
	private static final Container<Integer> nrEquivalenceQueries = new Container<>();
	private static final Container<Integer> nrUniqueEquivalenceQueries = new Container<>();
	private static final Container<Integer> nrResets = new Container<>();

	private static final List<Runnable> shutdownHooks = new ArrayList<>();

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
		SULConfig sul = readConfig(config, sutInterface);

		Log.setLogLevel(sul.logLevel);

		// first is the membership, second is the equivalence oracle
		System.out.println("Building membership oracle...");
		MembershipOracle<String, Word<String>> memOracle = buildMembershipOracle(sul);

		System.out.println("Building equivalence oracle...");
		EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle = buildEquivalenceOracle(learningParams, memOracle);

		System.out.println("Building Learner...");
		alphabet = SutInfo.generateInputAlphabet();
		SutInfo.generateOutputAlphabet();
		ExtensibleLStarMealy<String, String> learner = new ExtensibleLStarMealyBuilder<String, String>().withAlphabet(alphabet).withOracle(memOracle).create();

		System.out.println("Starting learner...");
		LearnResult learnResult = learn(learner, eqOracle);


		// final output to out.txt
		absTraceOut.println("Seed: " + learningParams.seed);
		errOut.println("Seed: " + learningParams.seed);
		absTraceOut.println("Done.");
		errOut.println("Successful run.");

		// output learned model with start state highlighted to dot file :
		// notes:
		// - make start state the only highlighted state in dot file
		// - learnlib makes highlighted state by setting attribute color='red'
		// on state

		writeOutputFiles(learnResult);

		errOut.println("Learner Finished!");

		// beeps to notify that learning is done :)
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

	private static void writeOutputFiles(LearnResult learnResult) {
		// output learned state machine as dot and pdf file :
		//File outputFolder = new File(outputDir + File.separator + learnResult.startTime);
		//outputFolder.mkdirs();
		File dotFile = new File(outputFolder.getAbsolutePath() + File.separator + "learnresult.dot");

		try (BufferedWriter out = new BufferedWriter(new FileWriter(dotFile))) {
			DotDo.write(learnResult.learnedModel, alphabet, out);
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
				statsOut.flush();

				closeOutputStreams();
				//InitCacheManager mgr = new InitCacheManager();
				//mgr.dump(outputDir + File.separator +  "cache.txt");
				if (!done) {
					SoundUtils.failure();
				}
			}
		});
	}

	private static LearnResult learn(LearningAlgorithm<MealyMachine<?,String,?,String>, String, Word<String>> learner,
									 EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle)
			throws LearningException, ObservationConflictException, IOException {
		LearnResult learnResult = new LearnResult();
		int hypCounter = 1;
		long endtmp;
		done = false;

		Log.fatal("Start Learning");
		absTraceOut.println("starting learning\n");
		//try {
			while (!done) {
				absTraceOut.println("");
				absTraceOut.flush();
				errOut.flush();

				try {
					// execute membership queries
					learner.startLearning();
					absTraceOut.flush();
					errOut.flush();
					absTraceOut.println("done learning");
					absTraceOut.flush();

					// stable hypothesis after membership queries
					MealyMachine<?, String, ?, String> hyp = learner.getHypothesisModel();
					String hypFileName = outputDir + File.separator + "tmp-learnresult"
							+ hypCounter + ".dot";
					String hypPdfFileName = outputDir + File.separator + "tmp-learnresult"
							+ hypCounter + ".pdf";

					File hypPDF = new File(hypPdfFileName);
					DotDo.writeDotFile(hyp, alphabet, hypFileName);
					DotUtil.invokeDot(hypFileName, "pdf", hypPDF);

					absTraceOut.println("starting equivalence query");
					absTraceOut.flush();
					errOut.flush();
					// search for counterexample
					DefaultQuery<String, Word<String>> o = null;
					o = eqOracle.findCounterExample(hyp, alphabet);

					absTraceOut.flush();
					errOut.flush();
					absTraceOut.println("done equivalence query");

					// no counter example -> learning is done
					if (o == null) {
						done = true;
						continue;
					}
					o = MealyUtil.shortenCounterExample(hyp, o);
					assert o != null;

					hypCounter ++;
					absTraceOut.println("Sending CE to LearnLib.");
					absTraceOut.println("Counter Example: "
							+ o.toString());
					absTraceOut.flush();
					errOut.flush();
					// return counter example to the learner, so that it can use
					// it to generate new membership queries
					learner.refineHypothesis(o);
					absTraceOut.flush();
					errOut.flush();
				} catch (CacheInconsistencyException e) {
					throw e;
				}
			}
		learnResult.learnedModel = learner.getHypothesisModel();
		return learnResult;
	}

	private static void closeOutputStreams() {
		statsOut.close();
		absTraceOut.close();
		absAndConcTraceOut.close();
		learnOut.close();
		errOut.close();
	}

	private static MembershipOracle<String, Word<String>> buildMembershipOracle(SULConfig sulConfig) {
		System.out.println("Building SUL Oracle...");
		SUL<String, String> socketSUL = new SocketSUL(sulConfig);
		SULOracle<String, String> sulOracle = new SULOracle<>(socketSUL);

		System.out.println("Building Probabilistic Oracle...");
		double probFraction = (double) sulConfig.confidence / 100;
		MembershipOracle<String, Word<String>> probabilisticOracle = new ProbabilisticOracle(sulOracle, sulConfig.runsPerQuery, probFraction, sulConfig.maxAttempts);

		System.out.println("Building Cache Tree...");
		tree = readCacheTree(CACHE_FILE);
		if (tree == null) {
			tree = new ObservationTree();
		}

		System.out.println("Building Cache Oracle...");
		return new CacheReaderOracle(tree, probabilisticOracle);
	}

	private static EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> buildEquivalenceOracle(LearningParams learningParams, MembershipOracle<String, Word<String>> queryOracle) {
		List<EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>>> eqOracles = new ArrayList<>();

		Random random = new Random(learningParams.seed);
		RandomWordsEQOracle<MealyMachine<?, String, ?, String>, String, Word<String>> randEqOracle = new RandomWordsEQOracle<>(queryOracle, learningParams.minTraceLength, learningParams.maxTraceLength, learningParams.maxNumTraces, random);
		eqOracles.add(randEqOracle);

		if (learningParams.testTraces != null && !learningParams.testTraces.isEmpty()) {
            WordCheckingEquivalenceOracle wordEqOracle = new WordCheckingEquivalenceOracle(queryOracle, learningParams.testTraces);
            eqOracles.add(wordEqOracle);
        }

		EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle = null;
	    if (eqOracles.isEmpty()) {
	        throw new BugException("No equivalence oracle could be defined");
	    } else if (eqOracles.size() == 1) {
	        eqOracle = eqOracles.get(0);
	    } else {
	        eqOracle = new CompositeEquivalenceOracle(eqOracles);
	    }
		return eqOracle;
	}

	public static SULConfig readConfig(Config config, SutInterface sutInterface) {
		// read/disp config params for learner
		learningParams = config.learningParams;
		learningParams.printParams(absTraceOut);

		SutInfo.setInputSignatures(sutInterface.inputInterfaces);
		SutInfo.setOutputSignatures(sutInterface.outputInterfaces);

		LearnLog.addAppender(new PrintStreamLoggingAppender(LogLevel.DEBUG,
				learnOut));

		// read/disp SUT config
		SULConfig sut = config.sulConfig;
		sut.printParams(absTraceOut);
		return sut;
	}

	public static SutInterface createSutInterface(Config config)
			throws FileNotFoundException {
		File sutInterfaceFile = new File(sutConfigFile
				.getParentFile().getAbsolutePath()
				+ File.separator
				+ config.learningParams.sutInterface);
		InputStream sutInterfaceInput = new FileInputStream(sutInterfaceFile);
		Yaml yaml = new Yaml(new Constructor(SutInterface.class));
		return (SutInterface) yaml.load(sutInterfaceInput);
	}

	public static Config createConfig() throws FileNotFoundException {
		InputStream configInput = new FileInputStream(sutConfigFile);
		Yaml yaml = new Yaml(new Constructor(Config.class));
		return (Config) yaml.load(configInput);
	}

	public static void handleArgs(String[] args) {
		if (args.length == 0) {
			errOut.println("Use: java Main config_file");
			System.exit(-1);
		}
		sutConfigFile = new File(args[0]);
		if (!sutConfigFile.exists()) {
			errOut.println("The sut config file " + args[0]
					+ " does not exist");
			System.exit(-1);
		}
		sutConfigFile = sutConfigFile.getAbsoluteFile();
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
			output.writeObject(tree);
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


