package sharedconfig.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Тип сумма - содержит либо объект типа L, либо объект типа R
 * @param <L> левый тип типа-суммы, обычно содержит ошибку
 * @param <R> правый тип типа-суммы, обычно содержит результат
 */
public final class Either<L, R> {
    public static <L, R> Either<L, R> left(@NotNull L value) {
        if (value == null) {
            throw new IllegalArgumentException("value can't be null");
        }
        return new Either<>(value, null, false);
    }
    public static <L, R> Either<L,R> right(@NotNull R value) {
        if (value == null) {
            throw new IllegalArgumentException("value can't be null");
        }
        return new Either<>(null, value, true);
    }

    private final L left;
    private final R right;
    private final boolean isRight;

    private Either(@Nullable L l, @Nullable R r, boolean isRight) {
        this.left = l;
        this.right = r;
        this.isRight = isRight;
    }

    public <R2> Either<L, R2> map(Function<? super R, ? extends R2> func) {
        if (this.isRight) {
            return right(func.apply(this.right));
        }
        assert this.left != null;
        return left(this.left);
    }

    public <R2> Either<L, R2> flatMap(Function<? super R, ? extends Either<L, R2>> func) {
        if (this.isRight) {
            return func.apply(this.right);
        }
        assert this.left != null;
        return left(this.left);
    }

    public <L2, R2> Either<L2, R2> bimap(Function<? super L, ? extends L2> mapLeft, Function<? super R, ? extends R2> mapRight) {
        if (this.isRight) {
            return right(mapRight.apply(this.right));
        } else {
            return left(mapLeft.apply(this.left));
        }
    }

    public @NotNull R getRightOrThrow() throws RuntimeException {
        if (this.isRight) {
            assert this.right != null;
            return this.right;
        }
        if (this.left instanceof Exception) {
            var exception = (Exception)this.left;
            throw new RuntimeException(exception.getMessage(), exception);
        }
        throw new RuntimeException("No value present");
    }

    public Optional<@NotNull R> toOptional() {
        if (this.isRight)
            return Optional.ofNullable(this.right);
        return Optional.empty();
    }

    public Optional<@NotNull L> tryGetLeft() {
        if (this.isRight)
            return Optional.empty();
        return Optional.ofNullable(this.left);
    }

    public @NotNull L getLeft() {
        if (this.isRight)
            throw new NoSuchElementException("Value has 'right' type");
        assert this.left != null;
        return this.left;
    }

    public Optional<@NotNull R> tryGetRight() {
        if (this.isRight)
            return Optional.ofNullable(this.right);
        return  Optional.empty();
    }

    public @NotNull R getRight() {
        if (!this.isRight)
            throw new NoSuchElementException("Value has 'left' type");
        assert this.right != null;
        return this.right;
    }

    public boolean isLeft() {
        return !this.isRight;
    }

    public boolean isRight() {
        return this.isRight;
    }

    public static <R> Either<Exception, R> ofThrowable(Callable<R> func) {
        try {
            return Either.right(func.call());
        } catch (Exception e) {
            return Either.left(e);
        }
    }
}
