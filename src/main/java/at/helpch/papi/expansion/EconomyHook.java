package at.helpch.papi.expansion;


import com.hypixel.hytale.server.core.universe.PlayerRef;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EconomyHook extends VaultHook {

    private static final Pattern BALANCE_DECIMAL_POINTS_PATTERN = Pattern.compile("balance_(?<points>\\d+)dp");
    private static final ThreadLocal<DecimalFormat> COMMAS_FORMAT = ThreadLocal.withInitial(() -> new DecimalFormat("#,###"));
    private static final ThreadLocal<DecimalFormat> FIXED_FORMAT = ThreadLocal.withInitial(() -> new DecimalFormat("#"));
    private static final Map<Integer, ThreadLocal<DecimalFormat>> DECIMAL_FORMATS_CACHE = new ConcurrentHashMap<>();

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private final NavigableMap<BigDecimal, String> suffixes = new TreeMap<>();
    private Economy economy;

    public EconomyHook(VaultUnlockedExpansion expansion) {
        super(expansion);

        final ExpansionConfig.Formatting formatting = expansion.getExpansionConfig(VaultUnlockedExpansion.class).formatting();

        suffixes.put(THOUSAND, formatting.thousands());
        suffixes.put(BigDecimal.valueOf(1_000_000L), formatting.millions());
        suffixes.put(BigDecimal.valueOf(1_000_000_000L), formatting.billions());
        suffixes.put(BigDecimal.valueOf(1_000_000_000_000L), formatting.trillions());
        suffixes.put(BigDecimal.valueOf(1_000_000_000_000_000L), formatting.quadrilions());
        setup();
    }

    private BigDecimal getBalance(@NotNull final PlayerRef player) {
        return economy.getBalance("HelpChat:PlaceholderAPI", player.getUuid());
    }

    private @NotNull String setDecimalPoints(BigDecimal balance, int points) {
        final ThreadLocal<DecimalFormat> tl = DECIMAL_FORMATS_CACHE.computeIfAbsent(points, p -> ThreadLocal.withInitial(() -> {
            final DecimalFormat format = new DecimalFormat("0");
            format.setGroupingUsed(false);
            format.setMinimumFractionDigits(p);
            format.setMaximumFractionDigits(p);
            format.setRoundingMode(RoundingMode.DOWN);
            return format;
        }));

        return tl.get().format(balance);
    }

    /**
     * Format player's balance, 1200 -> 1.2K
     *
     * @param balance balance to format
     * @return balance formatted
     * @author <a href="https://stackoverflow.com/users/829571/assylias">assylias</a> (<a href="https://stackoverflow.com/a/30661479/11496439">source</a>)
     */
    private @NotNull String formatBalance(@NotNull final BigDecimal balance) {
        if (balance.signum() < 0) {
            return "-" + formatBalance(balance.negate());
        }

        if (balance.compareTo(THOUSAND) < 0) {
            return balance.toPlainString();
        }

        final Map.Entry<BigDecimal, String> e = suffixes.floorEntry(balance);
        final BigDecimal divideBy = e.getKey();
        final String suffix = e.getValue();

        final BigDecimal truncated = balance.divide(divideBy.divide(BigDecimal.TEN), 0, RoundingMode.DOWN);
        final boolean hasDecimal = truncated.compareTo(HUNDRED) < 0 &&
                truncated.remainder(BigDecimal.TEN).compareTo(BigDecimal.ZERO) != 0;
        final BigDecimal value = hasDecimal
                ? truncated.divide(BigDecimal.TEN, 1, RoundingMode.DOWN)
                : truncated.divide(BigDecimal.TEN, 0, RoundingMode.DOWN);

        return value.toPlainString() + suffix;
    }

    @Override
    public void setup() {
        economy = VaultUnlockedServicesManager.get().economyObj();
    }

    @Override
    public boolean isReady() {
        return economy != null;
    }

    @Override
    public @Nullable String onRequest(@Nullable PlayerRef offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }

        final BigDecimal balance = getBalance(offlinePlayer);

        if (params.startsWith("balance_")) {
            final Matcher matcher = BALANCE_DECIMAL_POINTS_PATTERN.matcher(params);

            if (matcher.find()) {
                final Integer points = tryParse(matcher.group("points"));

                if (points == null) {
                    return matcher.group("points") + " is not a valid number";
                }

                return setDecimalPoints(balance, points);
            }
        }

        return switch (params) {
            case "balance" -> setDecimalPoints(balance, Math.max(2, economy.fractionalDigits(economy.getName())));
            case "balance_fixed" -> FIXED_FORMAT.get().format(balance);
            case "balance_formatted" -> formatBalance(balance);
            case "balance_commas" -> COMMAS_FORMAT.get().format(balance);
            case "name" -> economy.getName();
            default -> null;
        };
    }
}
