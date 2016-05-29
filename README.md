# SMTIBEA
This work provides a hybrid multiobjective optimization algorithm that combines the IBEA (Indicator-Based Evolutionary Algorithm) with SMT (Satisfiability Modulo Theories) solving. 

SMTIBEA specifies an input problem as a set of quantifier-free formulas in first-order logic. Different types of variables, such as Boolean, integer and real, are supported. Moreover, rich theories, such as quantifier-free linear integer arithmetic and quantifier-free linear real arithmetic, are used for reasoning.

The details of the latest implemenation (Ver.201605) are as follows:
- the IBEA part follows the SATIBEA approach (ref: http://research.henard.net/SPL/ICSE_2015/) based on the jMetal 4.5 framework (ref: http://jmetal.sourceforge.net/)
- the SMT solving is with the Z3 solver 4.4 Java build (ref: https://github.com/Z3Prover/z3)
- the improved search follows the Guided Improvement Algorithm (GIA) used in EPOAL (https://github.com/jmguo/epoal)
