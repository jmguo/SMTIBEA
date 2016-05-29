/*
 * Author : Christopher Henard (christopher.henard@uni.lu)
 * Date : 01/03/14
 * Copyright 2013 University of Luxembourg 鈥�Interdisciplinary Centre for Security Reliability and Trust (SnT)
 * All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package smtibea;

import java.util.List;
import java.util.Random;
import jmetal.core.Problem;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.BinarySolutionType;
import jmetal.encodings.variable.Binary;

public class SMTIBEA_BinarySolution extends BinarySolutionType {

    private String fm;
    private int nFeat;
    private List<Integer> mandatoryFeaturesIndices, deadFeaturesIndices;
    int n = 0;
    private List<Integer> seed;
    private static Random r = new Random();

    public SMTIBEA_BinarySolution(Problem problem, int nFeat, String fm, List<Integer> mandatoryFeaturesIndices, List<Integer> deadFeaturesIndices, List<Integer> seed) {
        super(problem);
        this.fm = fm;
        this.nFeat = nFeat;
        this.mandatoryFeaturesIndices = mandatoryFeaturesIndices;
        this.deadFeaturesIndices = deadFeaturesIndices;
        this.seed = seed;
    }

    public Variable[] createVariables() {
        Variable[] vars = new Variable[problem_.getNumberOfVariables()];

        for (int i = 0; i < vars.length; i++) {
            Binary bin = new Binary(nFeat);

            for (int j = 0; j < bin.getNumberOfBits(); j++) {
                bin.setIth(j, r.nextBoolean());

            }

            for (Integer f : this.mandatoryFeaturesIndices) {
                bin.setIth(f, true);
            }

            for (Integer f : this.deadFeaturesIndices) {
                bin.setIth(f, false);
            }

            vars[i] = bin;

        }
        return vars;
    }

}
