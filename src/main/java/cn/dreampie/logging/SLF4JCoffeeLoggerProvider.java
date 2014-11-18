package cn.dreampie.logging;

class SLF4JCoffeeLoggerProvider implements LessLoggerProvider {
    public CoffeeLogger getLogger(Class<?> clazz) {
        return new SLF4JCoffeeLogger(org.slf4j.LoggerFactory.getLogger(clazz));
    }

    private static class SLF4JCoffeeLogger implements CoffeeLogger {
        private final org.slf4j.Logger logger;

        private SLF4JCoffeeLogger(org.slf4j.Logger logger) {
            this.logger = logger;
        }

        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        public void debug(String msg) {
            logger.debug(msg);
        }

        public void debug(String format, Object... args) {
            if( logger.isDebugEnabled() ) {
                logger.debug(String.format(format, args));
            }
        }

        public void info(String msg) {
            logger.info(msg);
        }

        public void info(String format, Object... args) {
            if( logger.isInfoEnabled() ) {
                logger.info( String.format(format, args) );
            }
        }

        public void error(String msg, Throwable t) {
            logger.error(msg, t);
        }

        public void error(String format, Object... args) {
            if( logger.isErrorEnabled() ) {
                logger.error( String.format(format, args) );
            }
        }

    }
}
