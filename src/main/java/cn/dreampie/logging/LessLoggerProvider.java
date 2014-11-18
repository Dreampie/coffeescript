package cn.dreampie.logging;

interface LessLoggerProvider {
    CoffeeLogger getLogger(Class<?> clazz);
}
