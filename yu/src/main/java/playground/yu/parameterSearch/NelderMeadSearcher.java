/**
 *
 */
package playground.yu.parameterSearch;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.SimpleScalarValueChecker;
import org.apache.commons.math.optimization.direct.NelderMead;

/**
 * An attempt to use Nelder-Mead Method to search the best parameters in scoring
 * function
 * 
 * @author C
 * 
 */
public class NelderMeadSearcher {

	/**
	 * @param args
	 * @throws IllegalArgumentException
	 * @throws FunctionEvaluationException
	 * @throws OptimizationException
	 */
	public static void main(String[] args) throws OptimizationException,
			FunctionEvaluationException, IllegalArgumentException {
		NelderMead optimizer = new NelderMead();
		optimizer.setMaxIterations(1000);
		optimizer.setMaxEvaluations(1000);

		optimizer.setConvergenceChecker(new SimpleScalarValueChecker(0.001,
				0.001));
		optimizer.optimize(new LLhParamFct(args[0]), GoalType.MAXIMIZE,
				new double[] { -4.5, -1d });
	}
}
