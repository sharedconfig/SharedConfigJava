package sharedconfig.helpers;

import lombok.SneakyThrows;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class OptionalHelper {
    public static <T> Optional<T> ofThrowable(Callable<T> func) {
        try {
            return Optional.ofNullable(func.call());
        } catch (Exception e){
            return Optional.empty();
        }
    }

    @SneakyThrows
    public static Optional<Object> ofThrowable(Runnable func) {
        try {
            func.run();
            return Optional.of(true);
        } catch (Exception e){
            return Optional.empty();
        }
    }
}
