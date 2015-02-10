//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains a reference to the log object used by this package.
 */
public class Log
{
    public static interface Target {
        void debug (String message, Object... args);
        void info (String message, Object... args);
        void warning (String message, Object... args);
    }

    /** We dispatch our log messages through this logger. */
    public static Target log = new Target() {
        public void debug (String message, Object... args) {
            log(Level.FINE, message, args);
        }
        public void info (String message, Object... args) {
            log(Level.INFO, message, args);
        }
        public void warning (String message, Object... args) {
            log(Level.WARNING, message, args);
        }
        private void log (Level level, String message, Object[] args) {
            Object exn = (args.length == 0) ? null : args[args.length-1];
            if (args.length % 2 == 1 && (exn instanceof Throwable)) {
                impl.log(level, format(message, args), (Throwable)exn);
            } else {
                impl.log(level, format(message, args));
            }
        }
        private Logger impl = Logger.getLogger("com.samskivert.depot");
    };

    /** Formats the supplied log message and arguments. */
    public static String format (String message, Object... args) {
        if (args.length < 2) return message;
        StringBuilder sb = new StringBuilder(message);
        sb.append(" [");
        for (int ii = 0; ii < args.length; ii += 2) {
            sb.append(args[ii]).append("=").append(args[ii+1]);
        }
        return sb.append("]").toString();
    }
}
