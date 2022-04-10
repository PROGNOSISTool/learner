package learner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealy;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealyBuilder;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.filter.cache.mealy.MealyCacheOracle;
import de.learnlib.filter.cache.mealy.MealyCaches;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.filter.statistic.oracle.CounterOracle;
import de.learnlib.oracle.equivalence.EQOracleChain;
import de.learnlib.oracle.equivalence.WpMethodEQOracle;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.util.mealy.MealyUtil;
import learner.oracles.ExtendedCounterOracle;
import learner.oracles.LogOracle;
import learner.oracles.ProbabilisticOracle;
import learner.oracles.SocketSULOracle;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import org.yaml.snakeyaml.Yaml;

import util.Chmod;
import util.FileManager;
import util.DotWriter;

public class Main {
	public static final String SUL_CACHE_FILE = "cache" + File.separator + "sul.ser";

	private static File sutConfigFile = null;
	private static SocketSUL sul;
	private static final long timeSnap = System.currentTimeMillis();
	public static String outputDir = "output" + File.separator + timeSnap;
	private static File outputFolder = null;
	private static final LearnLogger logger = LearnLogger.getLogger("Learner");
	private static boolean learning;
    public static Config config;
	private static Alphabet<String> alphabet;
    private static MealyCacheOracle<String, String> cacheOracle;
	private static Counter queryCounter;
	private static Counter membershipCounter;
	private static Counter equivalenceCounter;

	public static void main(String[] args) throws Exception {
		try{
			// wrapper around main to return a useful error code upon nondeterminism
			runLearner(args);
		} catch (Exception e) {
			logger.info("Running shutdown hook");
			MealyCacheOracle.MealyCacheOracleState<String, String> cacheState = cacheOracle.suspend();
			saveState(cacheState, SUL_CACHE_FILE);
			copyInputsToOutputFolder();
            sul.stop();
			e.printStackTrace();
			System.exit(42);
		}
	}

	public static void runLearner(String[] args) throws Exception {
		logger.logEvent("Reading program arguments");
		handleArgs(args);

		logger.logEvent("Setting up output directory...");
		setupOutput(outputDir);

		logger.logEvent("Reading config...");
		config = createConfig();
        alphabet = Alphabets.fromList(config.inputAlphabet);

		logger.logEvent("Building query oracle...");
		MembershipOracle<String, Word<String>> queryOracle = buildQueryOracle(config);

		logger.logEvent("Building membership oracle...");
		MembershipOracle<String, Word<String>> memOracle = buildMembershipOracle(queryOracle);

		logger.logEvent("Building equivalence oracle...");
		EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle = buildEquivalenceOracle(queryOracle);

		logger.logEvent("Building Learner...");
		learning = false;
        TTTLearnerMealy<String, String> learner = new TTTLearnerMealyBuilder<String, String>()
            .withAlphabet(alphabet)
            .withOracle(memOracle)
            .create();

		logger.logEvent("Starting learner...");
        MealyMachine<?, String, ?, String> model = learn(learner, eqOracle);

		// final output to out.txt
		logger.logEvent("Done.");
		logger.logEvent("Successful run.");

		// Move our last results into the "final" folder, overwriting its content
        // if necessary, but never deleting the folder as it can be used by a container.
		outputDir = "output" + File.separator + "final";
		File finalFolder = new File(outputDir);
        if (finalFolder.exists()) {
            for (File file : Objects.requireNonNull(finalFolder.listFiles())) {
                file.delete();
            }
        }

        if (outputFolder.exists()) {
            for (File file : Objects.requireNonNull(outputFolder.listFiles())) {
                file.renameTo(new File(outputDir + File.separator + file.getName()));
            }
        }

		outputFolder.delete();
		outputFolder = finalFolder;

		logger.logStatistic(queryCounter);
		logger.logStatistic(membershipCounter);
		logger.logStatistic(equivalenceCounter);
		logger.logModel(model);

		writeOutputFiles(model);

		logger.logEvent("Learner Finished!");
	}

	private static void copyInputsToOutputFolder() {
		File inputFolder = sutConfigFile.getParentFile();
		if (!inputFolder.getName().equalsIgnoreCase("input")) {
			logger.error("Could not find input folder \"input\", so not copying ");
		    return;
		}
		Path srcInputPath = inputFolder.toPath();
		Path dstInputPath = outputFolder.toPath().resolve(inputFolder.getName());

		try {
			FileManager.copyFromTo(srcInputPath, dstInputPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void writeOutputFiles(MealyMachine<?, String, ?, String> model) {
		File dotFile = new File(outputFolder.getAbsolutePath() + File.separator + "learnresult.dot");

		try (BufferedWriter out = new BufferedWriter(new FileWriter(dotFile))) {
			DotWriter.write(model, alphabet, out);
		} catch (IOException e1) {
			logger.error("Could not write to dot file.");
		}
		try {
			Chmod.set(7, 7, 5, true, outputFolder.getAbsolutePath());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public static void setupOutput(final String outputDir) {
		outputFolder = new File(outputDir);
		outputFolder.mkdirs();
		registerShutdownHook(() -> {
			logger.info("Running shutdown hook");
			MealyCacheOracle.MealyCacheOracleState<String, String> cacheState = cacheOracle.suspend();
			saveState(cacheState, SUL_CACHE_FILE);
			copyInputsToOutputFolder();
            sul.stop();
		});
	}

	private static MealyMachine<?, String, ?, String> learn(LearningAlgorithm<MealyMachine<?,String,?,String>, String, Word<String>> learner,
									 EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle)
			throws IOException {
		int hypCounter = 1;
        boolean done = false;

		if (!learning) {
			logger.info("Started Learning");
			learner.startLearning();
		}

		while (!done) {
			// stable hypothesis after membership queries
			MealyMachine<?, String, ?, String> hyp = learner.getHypothesisModel();
			String hypFileName = outputDir + File.separator + "tmp-learnresult"
					+ hypCounter + ".dot";

			DotWriter.writeDotFile(hyp, alphabet, hypFileName);

			logger.logEvent("starting equivalence query");

			// search for counterexample
			DefaultQuery<String, Word<String>> o = eqOracle.findCounterExample(hyp, alphabet);
			logger.logEvent("completed equivalence query");

			// no counter example -> learning is done
			if (o == null) {
				done = true;
				continue;
			}
			o = MealyUtil.shortenCounterExample(hyp, o);
			assert o != null;

			hypCounter ++;
			logger.logEvent("Sending counterexample to LearnLib.");
			logger.logCounterexample(o.toString());
			// return counter example to the learner, so that it can use
			// it to generate new membership queries
			learner.refineHypothesis(o);
		}
		return learner.getHypothesisModel();
	}

	public static MembershipOracle<String, Word<String>> buildQueryOracle(Config learnerConfig) {
		System.out.println("Building Socket SUL Oracle...");
		sul = new SocketSUL(learnerConfig);
		SULOracle<String, String> sulOracle = new SocketSULOracle(sul);

		System.out.println("Building Counter Oracle...");
		LearnLogger queryLogger = LearnLogger.getLogger("Query Oracle");
		CounterOracle<String, Word<String>> counterOracle = new ExtendedCounterOracle(queryLogger, sulOracle, "SUL Queries");
		queryCounter = counterOracle.getCounter();

		System.out.println("Building Log Oracle...");
		MembershipOracle<String, Word<String>> logOracle = new LogOracle(queryLogger, counterOracle);

		System.out.println("Building Probabilistic Oracle...");
		double probFraction = (double) learnerConfig.confidence / 100;
		MembershipOracle<String, Word<String>> probabilisticOracle = new ProbabilisticOracle(logOracle, learnerConfig.runsPerQuery, probFraction, learnerConfig.maxAttempts);

		System.out.println("Building Cache Tree...");
		MealyCacheOracle.MealyCacheOracleState<String, String> cacheState = FileManager.readStateFromFile(SUL_CACHE_FILE);

		System.out.println("Building Cache Oracle...");
		cacheOracle = MealyCaches.createDAGCache(alphabet, probabilisticOracle);;
		if (cacheState != null) {
			cacheOracle.resume(cacheState);
		}
		return cacheOracle;
	}

	private static MembershipOracle<String, Word<String>> buildMembershipOracle(MembershipOracle<String, Word<String>> queryOracle) {
		CounterOracle<String, Word<String>> counterOracle = new ExtendedCounterOracle(LearnLogger.getLogger("Membership Oracle"),  queryOracle, "Membership Queries");
		membershipCounter = counterOracle.getCounter();
		return counterOracle;
	}

	private static EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> buildEquivalenceOracle(MembershipOracle<String, Word<String>> queryOracle) {
		EQOracleChain<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracles = new EQOracleChain<>();

		CounterOracle<String, Word<String>> counterOracle = new ExtendedCounterOracle(LearnLogger.getLogger("Equivalence Oracle"), queryOracle, "Equivalence Queries");
		equivalenceCounter = counterOracle.getCounter();

		WpMethodEQOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle = new WpMethodEQOracle<>(counterOracle, 2);
		eqOracles.addOracle(eqOracle);

		return eqOracles;
	}

	public static Config createConfig() throws FileNotFoundException {
		InputStream configInput = new FileInputStream(sutConfigFile);
		Yaml yaml = new Yaml();
        Map<String, Object> parsed = yaml.load(configInput);
        return yaml.loadAs(yaml.dump(parsed.get("learner")), Config.class);
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

	public static <T> void saveState(T state, String filename) {
		FileManager.writeStateToFile(state, filename);
		if (outputFolder != null) {
			filename = outputDir + File.separator + filename;
			FileManager.writeStateToFile(state, filename);
		}
	}

	public static void registerShutdownHook(Runnable r) {
		Runtime.getRuntime().addShutdownHook(new Thread(r));
	}
}