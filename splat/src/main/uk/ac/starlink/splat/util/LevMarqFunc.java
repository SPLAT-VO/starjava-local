package uk.ac.starlink.splat.util;

/**
 * LevMarqFunc is an interface that defines a method for evaluating
 * a function that is to be fitted by a LevMarq object.
 *
 * @since $Date$
 * @since 03-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */

//  This could be improved by using a container object to retain all
//  the model parameters and results, avoiding passing dyda and
//  returning a separate result.

public interface LevMarqFunc
{
    /**
     * Evaluate the fit function at a given point. Also returns the
     * partial derivatives of the function with respect to the model
     * parameters.
     *
     * @param x the position at which the function should be
     *          evaluated.
     * @param a the current model parameters.
     * @param na the number of model parameters to use (<=a.length-1).
     * @param dyda the partial derivatives.
     * @return the function value.
     */
    public double eval( double x, double[] a, int na, double[] dyda );
}

