/*
 * Original Creator: Christopher Henard
 * Date: 01/03/14
 * 
 * Modifier: Jia Hui Liang & Jianmei Guo
 * Date: April-May 2016
 * 
 */

package smtibea;

import java.io.FileReader;
import jmetal.core.Solution;
import jmetal.encodings.solutionType.BinaryRealSolutionType;
import jmetal.encodings.solutionType.BinarySolutionType;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.Binary;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.NegativeLiteralSelectionStrategy;
import org.sat4j.minisat.orders.PositiveLiteralSelectionStrategy;
import org.sat4j.minisat.orders.RandomLiteralSelectionStrategy;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.reader.DimacsReader;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import jmetal.operators.mutation.Mutation;
import org.sat4j.core.VecInt;
import org.sat4j.specs.IVecInt;

public class SMTIBEA_NewMutation extends Mutation {

	private static Random r = new Random();
	private String fm;
	private FMToZ3 ftz;
	private int nFeat;
	private List<List<Integer>> constraints;

	/**
	 * Valid solution types to apply this operator
	 */
	private static final List VALID_TYPES = Arrays.asList(BinarySolutionType.class, BinaryRealSolutionType.class,
			IntSolutionType.class, SMTIBEA_BinarySolution.class);

	private Double mutationProbability_ = null;

	private static final int SATtimeout = 60;
	private static final long iteratorTimeout = 6000;

	/**
	 * Constructor Creates a new instance of the Bit Flip mutation operator
	 */
	public SMTIBEA_NewMutation(HashMap<String, Object> parameters, String fm, FMToZ3 ftz, int nFeat,
			List<List<Integer>> constraints) {
		super(parameters);
		if (parameters.get("probability") != null) {
			mutationProbability_ = (Double) parameters.get("probability");
		}
		this.fm = fm;
		this.ftz = ftz;
		this.nFeat = nFeat;
		this.constraints = constraints;

	}

	/**
	 * Perform the mutation operation
	 *
	 * @param probability
	 *            Mutation probability
	 * @param solution
	 *            The solution to mutate
	 * @throws JMException
	 */
	public void doMutation(double probability, Solution solution) throws JMException {

		Integer in = r.nextInt(50);

		if (in != 0) {

			try {
				if ((solution.getType().getClass() == BinarySolutionType.class)
						|| (solution.getType().getClass() == BinaryRealSolutionType.class)
						|| solution.getType().getClass() == SMTIBEA_BinarySolution.class) {
					for (int i = 0; i < solution.getDecisionVariables().length; i++) {
						// for (int j = 0; j < ((Binary)
						// solution.getDecisionVariables()[i]).getNumberOfBits();
						// j++) {
						for (Integer j : SMTIBEA_Problem.featureIndicesAllowedFlip) { // flip
																						// only
																						// not
																						// "fixed"
																						// features
							if (PseudoRandom.randDouble() < probability) {
								((Binary) solution.getDecisionVariables()[i]).bits_.flip(j);
							}
						}
					}

					for (int i = 0; i < solution.getDecisionVariables().length; i++) {
						((Binary) solution.getDecisionVariables()[i]).decode();
					}
				} // if
				else { // Integer representation
					for (int i = 0; i < solution.getDecisionVariables().length; i++) {
						if (PseudoRandom.randDouble() < probability) {
							int value = PseudoRandom.randInt((int) solution.getDecisionVariables()[i].getLowerBound(),
									(int) solution.getDecisionVariables()[i].getUpperBound());
							solution.getDecisionVariables()[i].setValue(value);
						} // if
					}
				} // else
			} catch (ClassCastException e1) {
				Configuration.logger_
						.severe("BitFlipMutation.doMutation: " + "ClassCastException error" + e1.getMessage());
				Class cls = java.lang.String.class;
				String name = cls.getName();
				throw new JMException("Exception in " + name + ".doMutation()");
			}

		} else {
			// 1. use SMT solving for SMTIBEAv1 and SMTIBEAv2
			for (int i = 0; i < solution.getDecisionVariables().length; i++) {
				boolean[] prod = randomProduct();
				Binary bin = (Binary) solution.getDecisionVariables()[i];
				for (int j = 0; j < prod.length; j++) {
					bin.setIth(j, prod[j]);
				}
			}

			// 2. use improved search for SMTIBEAv3
			// for (int i = 0; i < solution.getDecisionVariables().length; i++)
			// {
			// double totalCost = solution.getObjective(4);
			// int totalUsedBefore = solution.getNumberOfBits() - (int)
			// solution.getObjective(1)
			// - (int) solution.getObjective(2);
			// int totalDefect = (int) solution.getObjective(3);
			// Binary bin = (Binary) solution.getDecisionVariables()[i];
			// boolean[] prod = randomProductBetter(totalCost, totalUsedBefore,
			// totalDefect);
			// for (int j = 0; j < prod.length; j++) {
			// bin.setIth(j, prod[j]);
			// }
			// }

		}

	} // doMutation

	public int numViolatedConstraints(Binary b) {

		int s = 0;
		for (List<Integer> constraint : constraints) {
			boolean sat = false;

			for (Integer i : constraint) {
				int abs = (i < 0) ? -i : i;
				boolean sign = i > 0;
				if (b.getIth(abs - 1) == sign) {
					sat = true;
					break;
				}
			}
			if (!sat) {
				s++;
			}

		}

		return s;
	}

	public int numViolatedConstraints(Binary b, HashSet<Integer> blacklist) {

		// IVecInt v = bitSetToVecInt(b);
		int s = 0;
		for (List<Integer> constraint : constraints) {
			boolean sat = false;

			for (Integer i : constraint) {
				int abs = (i < 0) ? -i : i;
				boolean sign = i > 0;
				if (b.getIth(abs - 1) == sign) {
					sat = true;
				} else {
					blacklist.add(abs);
				}
			}
			if (!sat) {
				s++;
			}

		}

		return s;
	}

	public int numViolatedConstraints(boolean[] b) {

		// IVecInt v = bitSetToVecInt(b);
		int s = 0;
		for (List<Integer> constraint : constraints) {

			boolean sat = false;

			for (Integer i : constraint) {
				int abs = (i < 0) ? -i : i;
				boolean sign = i > 0;
				if (b[abs - 1] == sign) {
					sat = true;
					break;
				}
			}
			if (!sat) {
				s++;
			}

		}

		return s;
	}

	/**
	 * Executes the operation
	 *
	 * @param object
	 *            An object containing a solution to mutate
	 * @return An object containing the mutated solution
	 * @throws JMException
	 */
	public Object execute(Object object) throws JMException {
		Solution solution = (Solution) object;

		if (!VALID_TYPES.contains(solution.getType().getClass())) {
			Configuration.logger_.severe(
					"BitFlipMutation.execute: the solution " + "is not of the right type. The type should be 'Binary', "
							+ "'BinaryReal' or 'Int', but " + solution.getType() + " is obtained");

			Class cls = java.lang.String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".execute()");
		} // if

		doMutation(mutationProbability_, solution);
		return solution;
	} // execute

	public boolean[] randomProduct() {

		boolean[] prod = new boolean[nFeat];
		for (int i = 0; i < prod.length; i++) {
			prod[i] = r.nextBoolean();
		}

		Binary binary = ftz.solve();
		if (binary != null) {
			for (int i = 0; i < prod.length; i++) {
				prod[i] = binary.getIth(i);
			}
		}

		return prod;
	}

	public boolean[] randomProductBetter(double totalCost, int totalUsedBefore, int totalDefect) {
		boolean[] prod = new boolean[nFeat];
		for (int i = 0; i < prod.length; i++) {
			prod[i] = r.nextBoolean();
		}

		Binary binary = ftz.solveBetterGIA(totalCost, totalUsedBefore, totalDefect);
		if (binary != null) {
			for (int i = 0; i < prod.length; i++) {
				prod[i] = binary.getIth(i);
			}
		}

		return prod;
	}

}
