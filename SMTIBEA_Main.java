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
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.encodings.variable.Binary;
import satibea.SATIBEA_Problem;
import satibea.SATIBEA_SettingsIBEA;

public class SMTIBEA_Main {

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) throws Exception {

		try {
			
			String fm = args[0];
			String augment = fm + ".augment";
			String dead = fm + ".dead";
			String mandatory = fm + ".mandatory";
			String seed = fm + ".richseed";
			
			int duration = Integer.parseInt(args[1]);

			Problem p = new SMTIBEA_Problem(fm, augment, mandatory, dead, seed);

			Algorithm a = new SMTIBEA_SettingsIBEA(p).configureSATIBEA(duration, fm,
					((SMTIBEA_Problem) p).getNumFeatures(), ((SMTIBEA_Problem) p).getConstraints());

			SolutionSet pop = a.execute();

			for (int i = 0; i < pop.size(); i++) {
				Variable v = pop.get(i).getDecisionVariables()[0];
				System.out.println("Conf" + (i + 1) + ": " + (Binary) v + " ");

			}

			for (int i = 0; i < pop.size(); i++) {
				Variable v = pop.get(i).getDecisionVariables()[0];
				for (int j = 0; j < pop.get(i).getNumberOfObjectives(); j++) {
					System.out.print(pop.get(i).getObjective(j) + " ");
				}
				System.out.println("");
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(
					"Usage: java -jar satibea.jar fmDimacs timeMS\nThe .augment, .dead, .mandatory and .richseed files should be in the same directory as the FM.");
		}
	}
}
