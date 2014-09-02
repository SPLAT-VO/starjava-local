package uk.ac.starlink.ttools.plot2.task;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;

/**
 * Used for obtaining a parameter qualified by an optional suffix.
 *
 * <p>In some cases, it is desirable to allow parameters which are
 * logically associated with a layer suffix to be specified without
 * the suffix, for instance if you have three layers using the
 * same table <code>x</code>, it's nice to write "<code>in=x</code>"
 * as a shorthand for "<code>in1=x in2=x in3=x</code>".
 * But if <code>in2=x</code> is present in the environment, that will
 * be used in preference.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2014
 */ 
public abstract class ParameterFinder<P extends Parameter> { 
    
    /**
     * Concrete subclasses must implement this method to create a
     * parameter of the right type with an arbitrary suffix.
     *                                       
     * @param  suffix  arbitrary suffix
     * @return  parameter of with the given suffix
     */ 
    protected abstract P createParameter( String suffix );

    /**
     * Returns a parameter to use for obtaining a value associated
     * with the given layer suffix from the given environment.
     * If the environment contains a value for the parameter
     * with the given suffix, or of any shortened form of that suffix
     * (including the empty string), that parameter is returned.
     * Otherwise, a parameter with the full suffix is returned.
     * In the latter case, the environment doesn't already have a
     * value for the parameter, but it may take steps to obtain one
     * (like asking the user).
     *
     * @param  env  execution environment, possibly populated with values
     * @param  fullSuffix  suffix associated with the layer for which
     *                     the value is required
     * @return  parameter for obtaining a value associated with the
     *          layer suffix
     */
    P getParameter( Environment env, String fullSuffix ) {
        Collection<String> names =
            new HashSet<String>( Arrays.asList( env.getNames() ) );
        for ( int i = fullSuffix.length(); i >= 0; i-- ) {
            P param = createParameter( fullSuffix.substring( 0, i ) );
            if ( names.contains( param.getName() ) ) {
                return param;
            }
        }
        return createParameter( fullSuffix );
    }
}

