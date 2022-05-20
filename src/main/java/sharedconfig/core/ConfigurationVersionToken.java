package sharedconfig.core;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.helpers.StringHelper;

import java.util.*;

/* package */ class ConfigurationVersionToken {
    @Getter private final long zit;
    @Getter private final @NotNull List<Long> changeIds;
    @Getter private final long maxId;
    @Getter private final long minId;

    public ConfigurationVersionToken(long zit, @Nullable List<Long> changeIds) {
        this.zit = zit;

        if (changeIds == null || changeIds.size() == 0) {
            this.changeIds = Collections.emptyList();
            this.maxId = 0;
            this.minId = 0;
            return;
        }

        this.changeIds = changeIds;

        var max = changeIds.get(0);
        var min = changeIds.get(0);
        for (Long id : changeIds) {
            min = Math.min(max, id);
            max = Math.max(max, id);
        }
        this.maxId = max;
        this.minId = min;
    }

    public static Optional<ConfigurationVersionToken> tryParse(@Nullable String input) {
        if (StringHelper.isNullOrEmpty(input))
            return Optional.empty();

        var zitPart = input.indexOf(':');
        if (zitPart < 0)
            return Optional.empty();

        var zit = StringHelper.tryToLong(input.substring(0, zitPart)).orElse(null);
        if (zit == null)
            return Optional.empty();

        long accum = 0;
        var changeIds = new ArrayList<Long>();
        for (var offset = zitPart + 1; offset < input.length(); ++offset) {
            var currentChar = input.charAt(offset);
            if (currentChar == ',') {
                changeIds.add(accum);
                accum = 0;
            } else if (Character.isDigit(currentChar)) {
                accum = 10 * accum + (currentChar - '0');
            } else {
                return Optional.empty();
            }
        }

        return Optional.of(new ConfigurationVersionToken(zit, changeIds));
    }

    @Override
    public String toString() {
        var changeIdsSize = changeIds.size();
        if (changeIdsSize == 0) {
            return Long.toString(zit);
        }
        if (changeIdsSize == 1) {
            return zit + ":" + changeIds.get(0);
        }

        var builder = new StringBuilder(Long.toString(zit));
        builder.append(":");
        for (int i = 0; i < changeIdsSize; ++i) {
            builder.append(changeIds.get(0));
            if (i != changeIdsSize - 1)
                builder.append(",");
        }
        return builder.toString();
    }
}
