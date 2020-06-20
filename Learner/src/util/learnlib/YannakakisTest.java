package util.learnlib;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import sutInterface.SutInfo;
import util.LearnlibUtils;
import util.ObservationTree;
import util.Tuple2;


import learner.Main;
import learner.YannakakisWrapper;

public class YannakakisTest {
    public static int MAX_NUM = 100000;
    public static int SEED_MIN = 0;
    public static int SEED_MAX = 10;
    public static String TEST_FILE = "testcases.txt";
    public static ObservationTree<String> root;
    public static int seed;

    public <O> void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java dotFile yannakakisCmd");
            return;
        }

        String dotFile = args[0];
        String yannakakisCmd = args[1];
        MealyMachine<?, String, ?, O> readAutomaton = DotDo.readDotFile(dotFile);
        Alphabet<String> alphabet = SutInfo.generateInputAlphabet();

        root = Main.readCacheTree(Main.CACHE_FILE);
        Tuple2<List<LinkedList<String>>, Integer> tuple2 = getMinimumalTestSuite(readAutomaton, alphabet, root, yannakakisCmd, SEED_MIN, SEED_MAX);
        System.out.println("Best seed:" + seed);
    }

    /**
     * Instantiates a yannakakis test generation command for seeds from minSeed to maxSeed and picks the best seed out of the lot,
     * that is, the seed for which the suite generated would require the fewest runs on the sut.
     *
     */
    public static <O> Tuple2<List<LinkedList<String>>,Integer>  getMinimumalTestSuite(MealyMachine<?, String, ?, O> automaton, Collection<? extends String> inputs, ObservationTree<String> root, String yanCmd, int minSeed, int maxSeed) throws IOException{
        seed = -1;
        int minCount = MAX_NUM;
        Node testNode = null;
        Node minimalTestNode = null;

        for (int i = minSeed; i < maxSeed; i ++) {
            String yannakakisCmdWithSeed = changeSeed(yanCmd, i);
            testNode = getTestSuiteTree(automaton, inputs, root, yannakakisCmdWithSeed, MAX_NUM);
            int suitSize = testNode.getCount();
            if (suitSize < minCount) {
               minCount = suitSize;
               minimalTestNode = testNode;
               seed = i;
            }
            System.out.println("Number of tests generated for seed " + i +": " + suitSize);
        }

        return new Tuple2<List<LinkedList<String>>,Integer> (minimalTestNode.getTests(), minimalTestNode.getCount());
    }

    /**
     * Runs a yannakakisCmd for test generation on the provided automaton. The tests generated are distributed in
     * a tree. A count is kept of the actual number of tests that would be run on the SUT. This is done by checking if the tests generated
     * are included in the ObservationTree, and if so, decrementing a counter.
     *
     * TODO The count should also be decremented when a new test is covered (that is, it is already included in the test tree), or when
     * a new test covers tests already present in the test tree.
     */
    private static <O> Node getTestSuiteTree(MealyMachine<?, String, ?, O> readAutomaton, Collection<? extends String> inputs, ObservationTree<String> root, String yannakakisCmd, int maxCount) throws IOException {
        YannakakisWrapper<O> wrapper = new YannakakisWrapper<O>(readAutomaton, inputs, yannakakisCmd);
        Node testTree = new Node();

        wrapper.initialize();
        PrintStream out = new PrintStream(
                new FileOutputStream(TEST_FILE, false));
        System.setErr(out);

        int count = 0;
        List<String> nextTest = wrapper.nextTest();

        while (nextTest != null && !nextTest.isEmpty() && (++ count) < maxCount) {
            testTree.addTest(nextTest);
            boolean observed = root.getObservation(LearnlibUtils.symbolsToWords(nextTest)) != null;
            if (observed) {
                count --;
            }
            nextTest = wrapper.nextTest();
        }

        out.close();
        wrapper.terminate();
        testTree.setCount(count);

        return testTree;
    }

    /**
     * Replaces the seed used in running the yannakakis command with a different one.
     */
    private static String changeSeed(String yanCmd, int newSeed) {
        String[] split = yanCmd.split("\\s");
        int i = 0;
        while (!split[i].contains("seed") && i < split.length) {
            i ++;
        }
        if (i == split.length) {
            System.out.println("Cannot change seed hence command remains unchanged");
        } else {
            split[i+1] = String.valueOf(newSeed);
            yanCmd = "";
            for (i=0; i < split.length; i++) {
                yanCmd += split[i] + " ";
            }
            yanCmd = yanCmd.trim();
        }
        return yanCmd;
    }

    public static class Node {
        private Map<String, Node> children = new HashMap<String, Node>();
        private int count = Integer.MAX_VALUE;

        public void setCount(int count) {
            this.count = count;
        }

        public int getCount() {
            return this.count;
        }

        public List<LinkedList<String>> getTests() {
            List<LinkedList<String>> tests = new ArrayList<LinkedList<String>>();
            if (children.isEmpty()) {
                tests.add(new LinkedList<String>());
                return tests;
            } else {
                for (String input : children.keySet()) {
                    Node child = children.get(input);
                    List<LinkedList<String>> generatedTests = child.getTests();
                    for (LinkedList<String> testCase : generatedTests) {
                        testCase.addFirst(input);
                    }

                    tests.addAll(generatedTests);
                }
                return tests;
            }
        }

        public boolean addTest(List<String> test) {
            if (test.isEmpty()) {
                return false;
            } else {
                List<String> remainingTest = new ArrayList<String>(test);
                String nextInput = remainingTest.remove(0);
                Node nextNode = children.get(nextInput);
                if (nextNode == null) {
                    nextNode = new Node();
                    children.put(nextInput, nextNode);
                    nextNode.addTest(remainingTest);
                    return true;
                } else {
                    return nextNode.addTest(remainingTest);
                }
            }
        }

        public boolean query(List<String> test) {
            if (test.isEmpty()) {
                return true;
            } else {
                List<String> remainingTest = new ArrayList<String>(test);
                String nextInput = remainingTest.remove(0);
                if (children.containsKey(nextInput)) {
                    return children.get(nextInput).query(remainingTest);
                } else {
                    return false;
                }
            }
        }
    }
}
