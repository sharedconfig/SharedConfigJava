package sharedconfig.helpers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class StringHelper {
    @Contract("null -> true")
    public static boolean isNullOrEmpty(@Nullable String s) {
        return s == null || s.length() == 0;
    }

    @Contract("null -> true")
    public static boolean isNullOrWhitespace(@Nullable String s) {
        return s == null || s.length() == 0 || isWhitespace(s);
    }

    public static @NotNull String removeStart(@NotNull final String str, @Nullable final String remove) {
        if (isNullOrEmpty(remove)) {
            return str;
        }
        if (str.startsWith(remove)){
            return str.substring(remove.length());
        }
        return str;
    }

    @Contract("null,null -> true; null,!null -> false; !null,null -> false")
    public static boolean equalsIgnoreCase(@Nullable String first, @Nullable String second) {
        if (first == null) {
            return second == null;
        } else {
            return first.equalsIgnoreCase(second);
        }
    }

    @Contract("null,null -> true; null,!null -> false; !null,null -> false")
    public static boolean equals(@Nullable String first, @Nullable String second) {
        if (first == null) {
            return second == null;
        } else {
            return first.equals(second);
        }
    }

    public static Optional<@NotNull Long> tryToLong(@Nullable String s) {
        try {
            if (s == null) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(s));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static int hashCodeIgnoreCase(@Nullable String s) {
        if (s == null)
            return 0;

        int hash = 0;
        int len = s.length();
        for (int i = 0; i < len; ++i) {
            char c = Character.toUpperCase(s.charAt(i));
            hash = 31 * hash + c;
        }
        return hash;
    }


    private static boolean isWhitespace(String s) {
        int length = s.length();
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                if (!Character.isWhitespace(s.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
