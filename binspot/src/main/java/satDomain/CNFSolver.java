/**
 * A system of proposition over tagged types.
 *
 */
package satDomain;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

/**
 * @author Axel Simon
 * @author hsi
 *
 */
class CNFSolver {
  private ISolver solver;

  CNFSolver (int numVars) {
    solver = SolverFactory.newDefault();
    final int resVars = solver.newVar(numVars);
    assert resVars == numVars;
  }

  void addClause (Clause clause) {
    if (solver != null)
      try {
        addClauseUnchecked(clause);
      } catch (final ContradictionException e) {
        solver = null;
      }
  }

  boolean checkSat () {
    if (solver == null)
      return false;
    try {
      return solver.isSatisfiable();
    } catch (final TimeoutException e) {
      throw new UnexpectedComplexity("checkSat");
    }
  }

  private void addClauseUnchecked (Clause clause)
      throws ContradictionException {
    solver.addClause(clause.toVecInt());
  }
}
