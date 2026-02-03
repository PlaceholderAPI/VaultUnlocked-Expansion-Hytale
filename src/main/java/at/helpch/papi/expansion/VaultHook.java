package at.helpch.papi.expansion;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class VaultHook {

    protected final VaultUnlockedExpansion expansion;

    public VaultHook(VaultUnlockedExpansion expansion) {
        this.expansion = expansion;
    }

    protected abstract void setup();

    public abstract boolean isReady();

    public abstract @Nullable String onRequest(@Nullable PlayerRef offlinePlayer, @NotNull String params);

    @Nullable
    protected static Integer tryParse(@Nullable final String value) {
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {}

        return null;
    }
}
