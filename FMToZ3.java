/*
 * Creator: Jia Hui Liang 
 * Date : April 2016
 * 
 * Modifier: Jianmei Guo
 * Date: May 2016
 * 
 */

package smtibea;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Global;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Params;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.Solver;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import jmetal.encodings.variable.Binary;

public class FMToZ3 {

    private final Context ctx;
    private final Solver solver;
    private BoolExpr[] vars = null;
    private RealExpr total_cost = null;
    private IntExpr total_used_before = null;
    private IntExpr total_defect = null;

    public FMToZ3() {
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        cfg.put("auto-config", "false");
        
        Random randomGenerator = new Random();
        int randomInt = randomGenerator.nextInt(500000000);
        Global.setParameter("smt.random_seed", Integer.toString(randomInt));
        Global.setParameter("smt.phase_selection", "5");
        this.ctx = new Context(cfg);
        this.solver = ctx.mkSolver();
    }

    public void parseDimacs(Reader dimacs) throws IOException {
        BufferedReader in = new BufferedReader(dimacs);
        String line;

        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("c ") || line.isEmpty()) {
                continue;
            } else if (line.startsWith("p cnf ")) {
                int numVars = Integer.parseInt(line.split("\\s+")[2]);
                vars = new BoolExpr[numVars];
                for (int i = 0; i < vars.length; i++) {
                    vars[i] = ctx.mkBoolConst(Integer.toString(i + 1));
                }
            } else {
                String[] clauseLine = line.split("\\s+");
                if (!clauseLine[clauseLine.length - 1].equals("0")) {
                    throw new IOException("Expected line to end with 0");
                }
                BoolExpr[] clauseLits = new BoolExpr[clauseLine.length - 1];
                for (int i = 0; i < clauseLits.length; i++) {
                    int lit = Integer.parseInt(clauseLine[i]);
                    BoolExpr var = vars[Math.abs(lit) - 1];
                    clauseLits[i] = lit > 0 ? var : ctx.mkNot(var);
                }
                solver.add(ctx.mkOr(clauseLits));
            }
        }
    }

    public void parseMandatory(Reader mandatory) throws IOException {
        BufferedReader in = new BufferedReader(mandatory);
        String line;

        while ((line = in.readLine()) != null) {
            line = line.trim();

            if (!line.isEmpty()) {
                int var = Math.abs(Integer.parseInt(line));
                solver.add(vars[var - 1]);
            }
        }
    }

    public void parseDead(Reader dead) throws IOException {
        BufferedReader in = new BufferedReader(dead);
        String line;

        while ((line = in.readLine()) != null) {
            line = line.trim();

            if (!line.isEmpty()) {
                int var = Math.abs(Integer.parseInt(line));
                solver.add(ctx.mkNot(vars[var - 1]));
            }
        }
    }

    public void parseAugment(Reader augment) throws IOException {
        BufferedReader in = new BufferedReader(augment);
        String line;

        List<RealExpr> costs = new ArrayList<>();
        List<IntExpr> used_befores = new ArrayList<>();
        List<IntExpr> defects = new ArrayList<>();

        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            } else {
                String[] objectiveLine = line.split("\\s+");
                BoolExpr var = vars[Integer.parseInt(objectiveLine[0]) - 1];
                costs.add((RealExpr) ctx.mkITE(var, ctx.mkReal(objectiveLine[1]), ctx.mkReal(0)));
                used_befores.add((IntExpr) ctx.mkITE(var, ctx.mkInt(objectiveLine[2]), ctx.mkInt(0)));
                int used_before = Integer.parseInt( objectiveLine[2].trim() );
                if ( used_before == 1 ){
                	defects.add((IntExpr) ctx.mkITE(var, ctx.mkInt(objectiveLine[3]), ctx.mkInt(0)));
                }
            }
        }
    
        total_cost = (RealExpr) ctx.mkAdd(costs.toArray(new ArithExpr[]{}));
        total_used_before = (IntExpr) ctx.mkAdd(used_befores.toArray(new ArithExpr[]{}));
        total_defect = (IntExpr) ctx.mkAdd(defects.toArray(new ArithExpr[]{}));
    }

    
    // general SMT solving
    
    public Binary solve() {
        solver.push();
        try {
            Random randomGenerator = new Random();
            int randomInt = randomGenerator.nextInt(500000000);
            Global.setParameter("smt.random_seed", Integer.toString(randomInt));
        	Params p = ctx.mkParams();
        	// timeout 6000ms
        	p.add("timeout", 6000);
        	solver.setParameters(p);
        
            switch (solver.check()) {
                case SATISFIABLE:
                    Model m = solver.getModel();
                    Binary b = new Binary(vars.length);
                    for (int i = 0; i < vars.length; i++) {
                        switch (m.evaluate(vars[i], false).getBoolValue()) {
                            case Z3_L_FALSE:
                                b.setIth(i, false);
                                break;
                            case Z3_L_TRUE:
                                b.setIth(i, true);
                                break;
                            case Z3_L_UNDEF:
                                b.setIth(i, System.currentTimeMillis() % 2 == 0);
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                    }
                    return b;
                default:
                    return null;
            }
        } finally {
            solver.pop();
        }
    }
    
    
    // Improved search for SMTIBEAv3
    
    public Binary solveBetterGIA(double totalCost, int totalUsedBefore, int totalDefect) {
        solver.push();
        try {
        	
        	solver.add(ctx.mkLe(total_cost, ctx.mkReal(Double.toString(totalCost))),
					 ctx.mkLe(total_defect, ctx.mkReal(totalDefect)) );
        	
        	Params p = ctx.mkParams();
        	p.add("timeout", 6000);
        	solver.setParameters(p);
        	
            switch (solver.check()) {
                case SATISFIABLE:
                    Model m = solver.getModel();
                    Binary b = new Binary(vars.length);
                    for (int i = 0; i < vars.length; i++) {
                        switch (m.evaluate(vars[i], false).getBoolValue()) {
                            case Z3_L_FALSE:
                                b.setIth(i, false);
                                break;
                            case Z3_L_TRUE:
                                b.setIth(i, true);
                                break;
                            case Z3_L_UNDEF:
                                b.setIth(i, System.currentTimeMillis() % 2 == 0);
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                    }
                    return b;
                default:
                    return null;
            }
        } finally {
            solver.pop();
        }
    }
    
}
