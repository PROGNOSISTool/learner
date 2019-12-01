package sutInterface.tcp;


import learner.MembershipOracle;
import learner.EquivalenceOracle;
import sutInterface.CacheReaderOracle;
import sutInterface.CounterWrapper;
import sutInterface.NonDeterminismValidatorWrapper;
import sutInterface.ObservationTreeWrapper;
import sutInterface.ProbablisticNonDeterminismValidator;
import sutInterface.ProbablisticOracle;
import sutInterface.SutWrapper;
import sutInterface.UniqueQueryCounterWrapper;
import sutInterface.tcp.SutInterfaceBuilder.WrapperBuilder;
import sutInterface.tcp.init.LogOracleWrapper;
import util.Container;
import util.ObservationTree;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;

public class SutInterfaceBuilder {
	private Oracle current;
	private SutWrapper sutWrapper;
	
	private OracleBuilder oracleBuilder = new OracleBuilder();
	private WrapperBuilder wrapperBuilder = new WrapperBuilder();
	
	public OracleBuilder sutWrapper(SutWrapper sutWrapper) {
		SutInterfaceBuilder.this.sutWrapper = sutWrapper;
		return oracleBuilder;
	}
	
	private void setDefaults() {
		this.current = null;
		this.sutWrapper = null;
	}
	
	public SutInterfaceBuilder() {
		this.setDefaults();
	}
	
	public class OracleBuilder {
		private OracleBuilder() {}
		
		public WrapperBuilder equivalenceOracle(Container<Integer> equivCounter, Container<Integer> uniqueEquivCounter) {
			current = new EquivalenceOracle(sutWrapper, equivCounter, uniqueEquivCounter);
			return wrapperBuilder;
		}
		
		public WrapperBuilder equivalenceOracle(Container<Integer> equivCounter) {
			current = new EquivalenceOracle(sutWrapper, equivCounter);
			return wrapperBuilder;
		}
		
		public WrapperBuilder membershipOracle(Container<Integer> membershipCounter) {
			current = new MembershipOracle(sutWrapper, membershipCounter);
			return wrapperBuilder;
		}
	}
	
	/*
	 * Oracle eqOracleRunner =
				new NonDeterminismValidatorWrapper(10, 
				new ObservationTreeWrapper(tree, new LogOracleWrapper(new EquivalenceOracle(sutWrapper)))); //new LogOracleWrapper(new EquivalenceOracle(sutWrapper));
		Oracle memOracleRunner = 
				new NonDeterminismValidatorWrapper(10,
						new ObservationTreeWrapper(tree, new LogOracleWrapper( new MembershipOracle(sutWrapper))));
		
	 */
	public class WrapperBuilder {
		private WrapperBuilder() {}
		
		public WrapperBuilder runMultipleTimes(int times) {
			return probablisticOracle(times, 1, times);
		}
		
		public WrapperBuilder probablisticOracle(int minimumAttempts, double minimumFraction, int maximumAttempts) {
			current = new ProbablisticOracle(current, minimumAttempts, minimumFraction, maximumAttempts);
			return this;
		}
		
		public NonDeterminismResolverBuilder cacheWriter(ObservationTree tree) {
			Oracle afterCache = current;
			current = new ObservationTreeWrapper(tree, current);
			return new NonDeterminismResolverBuilder(afterCache);
		}
		
		public NonDeterminismResolverBuilder cacheReader(ObservationTree tree) {
			Oracle afterCache = current;
			current = new CacheReaderOracle(tree, current);
			return new NonDeterminismResolverBuilder(afterCache);
		}
		
		public NonDeterminismResolverBuilder cacheReaderWriter(ObservationTree tree) {
			Oracle afterCache = current;
			current = new ObservationTreeWrapper(tree, current);
			current = new CacheReaderOracle(tree, current);
			return new NonDeterminismResolverBuilder(afterCache);
		}
		
		public WrapperBuilder logger() {
			current = new LogOracleWrapper(current);
			return this;
		}
		
		public WrapperBuilder invCheck() {
			current = new InvCheckOracleWrapper(current);
			return this;
		}
		
		public WrapperBuilder resetCounter(Container<Integer> counter) {
			current = new CounterWrapper(current, counter, false, true);
			return this;
		}
		
		public WrapperBuilder queryCounter(Container<Integer> counter) {
			current = new CounterWrapper(current, counter, true, false);
			return this;
		}
		
		public Oracle learnerInterface() {
			Oracle result = current;
			setDefaults();
			return result;
		}

		public WrapperBuilder uniqueQueryCounter(Container<Integer> counter) {
			current = new UniqueQueryCounterWrapper(current, counter);
			return this;
		}
	}
	
	
	public class NonDeterminismResolverBuilder {
		private final Oracle afterCache;
		
		private NonDeterminismResolverBuilder(Oracle afterCache) {
			this.afterCache = afterCache;
		}
		
		public WrapperBuilder nonDeterminismValidator(int attempts, ObservationTree tree) {
			current = new NonDeterminismValidatorWrapper(attempts, tree, current, afterCache);
			return wrapperBuilder;
		}
		
		public WrapperBuilder prlobablisticNonDeterminismValidator(int attempts, double minimumFraction, ObservationTree tree) {
			current = new ProbablisticNonDeterminismValidator(attempts, minimumFraction, tree, current, afterCache);
			return wrapperBuilder;
		}
		
		public WrapperBuilder crashOnNonDeterminsm() {
			return wrapperBuilder;
		}
		
		public WrapperBuilder askOnNonDeterminsm(ObservationTree tree) {
			current = new NonDeterminismTreePrunerAsker(tree, current);
			return wrapperBuilder;
		}
		
		public WrapperBuilder crashAndPruneTree(ObservationTree tree) {
			current = new NonDeterminismTreePruner(tree, current);
			return wrapperBuilder;
		}
	}
}
