package cn.dreampie.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

class JULICoffeeLoggerProvider implements LessLoggerProvider {
    public CoffeeLogger getLogger(Class<?> clazz) {
        return new JULICoffeeLogger(Logger.getLogger(clazz.getName()));
    }

    private static class JULICoffeeLogger implements CoffeeLogger {
        private final Logger logger;

        private JULICoffeeLogger(Logger logger) {
            this.logger = logger;
        }

        public boolean isDebugEnabled() {
            return logger.isLoggable(Level.FINE);
        }

        public boolean isInfoEnabled() {
            return logger.isLoggable(Level.INFO);
        }

        public void debug(String msg) {
            logger.fine(msg);
        }

        public void debug(String format, Object... args) {
            if( isDebugEnabled() ) {
                logger.fine( String.format(format, args) );
            }
        }

        public void info(String msg) {
            logger.info(msg);
        }

        public void info(String format, Object... args) {
            if( isInfoEnabled() ) {
                logger.info(String.format(format,args));
            }
        }

        public void error(String msg, Throwable t) {
            logger.log(Level.SEVERE, msg, t);
        }

        public void error(String format, Object... args) {
            logger.severe(String.format(format,args));
        }
    }
}
