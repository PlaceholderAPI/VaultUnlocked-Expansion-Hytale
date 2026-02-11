package at.helpch.papi.expansion;

import at.helpch.placeholderapi.expansion.Cacheable;
import at.helpch.placeholderapi.expansion.Configurable;
import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaultUnlockedExpansion extends PlaceholderExpansion implements Cacheable, Configurable<ExpansionConfig> {
    private EconomyHook economyHook;
    private PermissionHook permissionHook;

    @NotNull
    @Override
    public String getIdentifier() {
        return "vault";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "HelpChat";
    }

    @NotNull
    @Override
    public String getVersion() {
        return "1.0.1";
    }

    @Nullable
    @Override
    public String getRequiredPlugin() {
        return "TheNewEconomy:VaultUnlocked";
    }

    @Override
    public void clear() {
        economyHook = null;
        permissionHook = null;
    }

    @Override
    public boolean canRegister() {
        economyHook = new EconomyHook(this);
        permissionHook = new PermissionHook(this);
        return economyHook.isReady() || permissionHook.isReady();
    }

    @Nullable
    @Override
    public String onPlaceholderRequest(final PlayerRef player, @NotNull final String params) {
        if (player == null) {
            return "";
        }

        if (economyHook.isReady() && params.startsWith("eco_")) {
            return economyHook.onRequest(player, params.replace("eco_", ""));
        }

        return (permissionHook.isReady()) ? permissionHook.onRequest(player, params) : null;
    }

    @NotNull
    @Override
    public Class<ExpansionConfig> provideConfigType() {
        return ExpansionConfig.class;
    }

    @NotNull
    @Override
    public ExpansionConfig provideDefault() {
        return new ExpansionConfig(new ExpansionConfig.Formatting("k", "M", "B", "T", "Q"));
    }
}
