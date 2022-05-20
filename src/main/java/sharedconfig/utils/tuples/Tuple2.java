package sharedconfig.utils.tuples;

import lombok.Value;

@Value
public class Tuple2<X, Y> {
    X first;
    Y second;

    public static <X, Y> Tuple2<X, Y> of(X first, Y second) {
        return new Tuple2<X, Y>(first, second);
    }
}
