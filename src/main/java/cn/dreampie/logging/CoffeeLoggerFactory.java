package cn.dreampie.logging;

public class CoffeeLoggerFactory {
    private static CoffeeLoggerFactory instance = new CoffeeLoggerFactory();
    private LessLoggerProvider loggerProvider;

    private CoffeeLoggerFactory() {
        try {
            Class.forName("org.slf4j.Logger");
            loggerProvider = new SLF4JCoffeeLoggerProvider();
        } catch(ClassNotFoundException ex) {
            loggerProvider = new JULICoffeeLoggerProvider();
        }
    }

    public static CoffeeLogger getLogger(Class<?> clazz) {
        return instance.loggerProvider.getLogger(clazz);
    }
}
