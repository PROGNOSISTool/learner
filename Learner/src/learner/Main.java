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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.filter.cache.mealy.MealyCacheOracle;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.filter.statistic.oracle.CounterOracle;
import de.learnlib.oracle.equivalence.EQOracleChain;
import de.learnlib.oracle.equivalence.MealyWpMethodEQOracle;
import de.learnlib.oracle.equivalence.RandomWordsEQOracle;
import de.learnlib.oracle.equivalence.WpMethodEQOracle;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.util.mealy.MealyUtil;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import sutInterface.*;
import sutInterface.quic.LearnResult;
import util.Chmod;
import util.FileManager;
import util.SoundUtils;
import util.exceptions.BugException;
import util.exceptions.CorruptedLearningException;
import util.learnlib.DotDo;

public class Main {
	public static final String CACHE_FILE = "cache.ser";

	private static File sutConfigFile = null;
	public static LearningParams learningParams;
	private static final long timeSnap = System.currentTimeMillis();;
	public static final String outputDir = "output" + File.separator + timeSnap;
	private static File outputFolder = null;
	private static LearnLogger logger;
	private static boolean done;
	public static Config config;
	private static Alphabet<String> alphabet;
	private static final List<Runnable> shutdownHooks = new ArrayList<>();
	private static MealyCacheOracle<String, String> cacheOracle;
	private static Counter queryCounter;
	private static Counter membershipCounter;
	private static Counter equivalenceCounter;

	public static void main(String[] args) throws Exception {
		try{
			// wrapper around main to return a useful error code upon nondeterminism
			runLearner(args);
		} catch (CorruptedLearningException e) {
			System.exit(42);
		}
	}

	public static void runLearner(String[] args) throws Exception {
		logger = LearnLogger.getLogger(Main.class);

		logger.logEvent("Reading program arguments");
		handleArgs(args);

		logger.logEvent("Setting up output directory...");
		setupOutput(outputDir);

		logger.logEvent("Reading config...");
		Config config = createConfig();
		Main.config = config;

		logger.logEvent("Creating SUT interface...");
		SutInterface sutInterface = createSutInterface(config);

		logger.logEvent("Reading TCP parameters...");
		SULConfig sul = readConfig(logger, config, sutInterface);
		alphabet = SutInfo.generateInputAlphabet();
		SutInfo.generateOutputAlphabet();

		logger.logEvent("Building query oracle...");
		MembershipOracle<String, Word<String>> queryOracle = buildQueryOracle(sul);

		logger.logEvent("Building membership oracle...");
		MembershipOracle<String, Word<String>> memOracle = buildMembershipOracle(queryOracle);

		logger.logEvent("Building equivalence oracle...");
		EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle = buildEquivalenceOracle(learningParams, queryOracle);

		logger.logEvent("Building Learner...");
		ExtensibleLStarMealy<String, String> learner = new ExtensibleLStarMealyBuilder<String, String>().withAlphabet(alphabet).withOracle(memOracle).create();

		logger.logEvent("Starting learner...");
		LearnResult learnResult = learn(learner, eqOracle);


		// final output to out.txt
		logger.logConfig("Seed: " + learningParams.seed);
		logger.logEvent("Done.");
		logger.logEvent("Successful run.");

		logger.logStatistic(queryCounter);
		logger.logStatistic(membershipCounter);
		logger.logStatistic(equivalenceCounter);
		logger.logModel(learnResult.learnedModel);

		// output learned model with start state highlighted to dot file :
		// notes:
		// - make start state the only highlighted state in dot file
		// - learnlib makes highlighted state by setting attribute color='red'
		// on state

		writeOutputFiles(learnResult);

		logger.logEvent("Learner Finished!");

		// beeps to notify that learning is done :)
		SoundUtils.success();
	}

	private static void copyInputsToOutputFolder() {
		File inputFolder = sutConfigFile.getParentFile();
		if (!inputFolder.getName().equalsIgnoreCase("input")) {
			logger.error("Could not find input folder \"input\", so not copying ");
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
			logger.error("Could not write to dot file.");
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
		registerShutdownHook(() -> {
			logger.info("Running shutdown hook");
			MealyCacheOracle.MealyCacheOracleState<String, String> cacheState = cacheOracle.suspend();
			logger.info("Cache size: " + cacheOracle.getCacheSize());
			writeCacheTree(cacheState, true);
			copyInputsToOutputFolder();

			if (!done) {
				SoundUtils.failure();
			}
		});
	}

	private static LearnResult learn(LearningAlgorithm<MealyMachine<?,String,?,String>, String, Word<String>> learner,
									 EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle)
			throws IOException {
		LearnResult learnResult = new LearnResult();
		int hypCounter = 1;
		done = false;

		logger.info("Start Learning");
		learner.startLearning();

		while (!done) {
			// stable hypothesis after membership queries
			MealyMachine<?, String, ?, String> hyp = learner.getHypothesisModel();
			String hypFileName = outputDir + File.separator + "tmp-learnresult"
					+ hypCounter + ".dot";

			DotDo.writeDotFile(hyp, alphabet, hypFileName);

			logger.logEvent("starting equivalence query");

			// search for counterexample
			DefaultQuery<String, Word<String>> o = null;
			o = eqOracle.findCounterExample(hyp, alphabet);

			logger.logEvent("done equivalence query");

			// no counter example -> learning is done
			if (o == null) {
				done = true;
				continue;
			}
			o = MealyUtil.shortenCounterExample(hyp, o);
			assert o != null;

			hypCounter ++;
			logger.logEvent("Sending CE to LearnLib.");
			logger.logCounterexample(o.toString());
			// return counter example to the learner, so that it can use
			// it to generate new membership queries
			learner.refineHypothesis(o);
		}
		learnResult.learnedModel = learner.getHypothesisModel();
		return learnResult;
	}

	public static MembershipOracle<String, Word<String>> buildQueryOracle(SULConfig sulConfig) {
		System.out.println("Building Socket SUL Oracle...");
		SocketSUL socketSUL = new SocketSUL(sulConfig);
		SULOracle<String, String> sulOracle = new SocketSULOracle(socketSUL);

		System.out.println("Building Counter Oracle...");
		LearnLogger queryLogger = LearnLogger.getLogger("Query Oracle");
		CounterOracle<String, Word<String>> counterOracle = new ExtendedCounterOracle(queryLogger, sulOracle, "SUL Queries");
		queryCounter = counterOracle.getCounter();

		System.out.println("Building Log Oracle...");
		MembershipOracle<String, Word<String>> logOracle = new LogOracle(queryLogger, counterOracle);

		System.out.println("Building Probabilistic Oracle...");
		double probFraction = (double) sulConfig.confidence / 100;
		MembershipOracle<String, Word<String>> probabilisticOracle = new ProbabilisticOracle(logOracle, sulConfig.runsPerQuery, probFraction, sulConfig.maxAttempts);

		System.out.println("Building Cache Tree...");
		MealyCacheOracle.MealyCacheOracleState<String, String> cacheState = readCacheTree(CACHE_FILE);

		System.out.println("Building Cache Oracle...");
		cacheOracle = MealyCacheOracle.createDAGCacheOracle(alphabet, probabilisticOracle);
		if (cacheState != null) {
			cacheOracle.resume(cacheState);
			logger.info("Cache size: " + cacheOracle.getCacheSize());
		}
		return cacheOracle;
	}

	private static MembershipOracle<String, Word<String>> buildMembershipOracle(MembershipOracle<String, Word<String>> queryOracle) {
		CounterOracle<String, Word<String>> counterOracle = new ExtendedCounterOracle(LearnLogger.getLogger("Membership Oracle"),  queryOracle, "Membership Queries");
		membershipCounter = counterOracle.getCounter();
		return counterOracle;
	}

	private static EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> buildEquivalenceOracle(LearningParams learningParams, MembershipOracle<String, Word<String>> queryOracle) {
		List<EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>>> eqOracles = new ArrayList<>();

		CounterOracle<String, Word<String>> counterOracle = new ExtendedCounterOracle(LearnLogger.getLogger("Equivalence Oracle"), queryOracle, "Equivalence Queries");
		equivalenceCounter = counterOracle.getCounter();

		Random random = new Random(learningParams.seed);
		RandomWordsEQOracle<MealyMachine<?, String, ?, String>, String, Word<String>> randEqOracle = new RandomWordsEQOracle<>(counterOracle, learningParams.minTraceLength, learningParams.maxTraceLength, learningParams.maxNumTraces, random);
		eqOracles.add(randEqOracle);

		if (learningParams.testTraces != null && !learningParams.testTraces.isEmpty()) {
            WordCheckingEquivalenceOracle wordEqOracle = new WordCheckingEquivalenceOracle(counterOracle, learningParams.testTraces);
            eqOracles.add(wordEqOracle);
        }

		EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle = null;
	    if (eqOracles.isEmpty()) {
	        throw new BugException("No equivalence oracle could be defined");
	    } else if (eqOracles.size() == 1) {
	        eqOracle = eqOracles.get(0);
	    } else {
	        eqOracle = new EQOracleChain<>(eqOracles);
	    }
		return eqOracle;
	}

	public static SULConfig readConfig(LearnLogger logger, Config config, SutInterface sutInterface) {
		// read/disp config params for learner
		learningParams = config.learningParams;
		learningParams.printParams(logger);

		SutInfo.setInputSignatures(sutInterface.inputInterfaces);
		SutInfo.setOutputSignatures(sutInterface.outputInterfaces);

		// read/disp SUT config
		SULConfig sul = config.sulConfig;
		sul.printParams(logger);
		return sul;
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
			logger.error("Use: java Main config_file");
			System.exit(-1);
		}
		sutConfigFile = new File(args[0]);
		if (!sutConfigFile.exists()) {
			logger.error("The sut config file " + args[0] + " does not exist");
			System.exit(-1);
		}
		sutConfigFile = sutConfigFile.getAbsoluteFile();
	}

	public static int cachedTreeNum = 0;

	public static void writeCacheTree(MealyCacheOracle.MealyCacheOracleState<String, String> cacheState, boolean isFinal) {

	    String cachePath = CACHE_FILE;
	    String indexedCachePath = cachedTreeNum + CACHE_FILE;
		writeCacheTree(cacheState, isFinal ? cachePath : indexedCachePath);
		if (outputFolder != null) {
    		cachePath = outputDir + File.separator + cachePath;
    		indexedCachePath = outputDir + File.separator + indexedCachePath;
    		writeCacheTree(cacheState, isFinal ? cachePath : indexedCachePath);
		}
	}

	public static void writeCacheTree(MealyCacheOracle.MealyCacheOracleState<String, String> cacheState, String fileName) {
		if (cacheState == null) {
			logger.error("Could not write uninitialized cache state.");
			return;
		}
		try (
				OutputStream file = new FileOutputStream(fileName);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);
				) {
			output.writeObject(cacheState);
		}
		catch (IOException ex){
			System.err.println("Could not write cache state.");
		}
		Main.cachedTreeNum += 1;
	}

	public static MealyCacheOracle.MealyCacheOracleState<String, String> readCacheTree(String fileName) {
		try(
				InputStream file = new FileInputStream(fileName);
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream (buffer);
				) {
			@SuppressWarnings("unchecked")
			MealyCacheOracle.MealyCacheOracleState<String, String> cacheState =
					(MealyCacheOracle.MealyCacheOracleState<String, String>) input.readObject();
			return cacheState;
		}
		catch(ClassNotFoundException ex) {
			System.err.println("Cache state file corrupt");
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


