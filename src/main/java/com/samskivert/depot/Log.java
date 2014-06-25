//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import com.samskivert.util.Logger;

/**
 * Contains a reference to the log object used by this package.
 */
public class Log
{
    /** We dispatch our log messages through this logger. */
    public static Logger log = Logger.getLogger("com.samskivert.depot");
}
