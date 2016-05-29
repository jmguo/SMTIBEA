/*
 * Original Creator: Christopher Henard
 * Date: 01/03/14
 * 
 * Modifier: Jia Hui Liang & Jianmei Guo
 * Date: April-May 2016
 * 
 */

package smtibea;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.experiments.Settings;
import jmetal.metaheuristics.ibea.IBEA;
import jmetal.operators.selection.BinaryTournament;
import jmetal.util.JMException;
import jmetal.util.comparators.FitnessComparator;

import java.util.HashMap;
import java.util.List;
import jmetal.core.Problem;

/**
 * Settings class of algorithm IBEA
 */
public class SMTIBEA_SettingsIBEA extends Settings {

	public int populationSize_;
	public int maxEvaluations_;
	public int archiveSize_;

	public double mutationProbability_;
	public double crossoverProbability_;

	public double crossoverDistributionIndex_;
	public double mutationDistributionIndex_;

	/**
	 * Constructor
	 */
	public SMTIBEA_SettingsIBEA(Problem p) {
		super(p.getName());

		problem_ = p;
		// Default experiments.settings

	} // IBEA_Settings

	public Algorithm configureSATIBEA(long maxRunTimeMS, String fm, int numFeat, List<List<Integer>> constr)
			throws JMException {

		populationSize_ = 300;
		archiveSize_ = 300;

		mutationProbability_ = 0.001;
		crossoverProbability_ = 0.05;

		Algorithm algorithm;
		Operator selection;
		Operator crossover;
		Operator mutation;

		HashMap parameters; // Operator parameters

		algorithm = new IBEATimeLimited(problem_, maxRunTimeMS);

		// Algorithm parameters
		algorithm.setInputParameter("populationSize", populationSize_);
		algorithm.setInputParameter("maxEvaluations", maxEvaluations_);
		algorithm.setInputParameter("archiveSize", archiveSize_);

		// Mutation and Crossover for Real codification
		parameters = new HashMap();
		parameters.put("probability", crossoverProbability_);
		crossover = new SMTIBEA_SinglePointCrossover(parameters);

		parameters = new HashMap();
		parameters.put("probability", mutationProbability_);
		mutation = new SMTIBEA_NewMutation(parameters, fm, ((SMTIBEA_Problem) problem_).ftz, numFeat, constr);

		/* Selection Operator */
		parameters = new HashMap();
		parameters.put("comparator", new FitnessComparator());
		selection = new BinaryTournament(parameters);

		// Add the operators to the algorithm
		algorithm.addOperator("crossover", crossover);
		algorithm.addOperator("mutation", mutation);
		algorithm.addOperator("selection", selection);

		return algorithm;
	}

	/**
	 * Configure IBEA with user-defined parameter experiments.settings
	 *
	 * @return A IBEA algorithm object
	 * @throws jmetal.util.JMException
	 */
	public Algorithm configure() throws JMException {
		Algorithm algorithm;
		Operator selection;
		Operator crossover;
		Operator mutation;

		populationSize_ = 100;
		maxEvaluations_ = 1000;
		archiveSize_ = 100;

		mutationProbability_ = 0.05;
		crossoverProbability_ = 0.9;

		HashMap parameters; // Operator parameters

		algorithm = new IBEA(problem_);

		// Algorithm parameters
		algorithm.setInputParameter("populationSize", populationSize_);
		algorithm.setInputParameter("maxEvaluations", maxEvaluations_);
		algorithm.setInputParameter("archiveSize", archiveSize_);

		// Mutation and Crossover for Real codification
		parameters = new HashMap();
		parameters.put("probability", crossoverProbability_);
		crossover = new SMTIBEA_SinglePointCrossover(parameters);

		parameters = new HashMap();
		parameters.put("probability", mutationProbability_);
		mutation = new SMTIBEA_BitFlipMutation(parameters);

		/* Selection Operator */
		parameters = new HashMap();
		parameters.put("comparator", new FitnessComparator());
		selection = new BinaryTournament(parameters);

		// Add the operators to the algorithm
		algorithm.addOperator("crossover", crossover);
		algorithm.addOperator("mutation", mutation);
		algorithm.addOperator("selection", selection);

		return algorithm;
	} // configure

} // IBEA_Settings
