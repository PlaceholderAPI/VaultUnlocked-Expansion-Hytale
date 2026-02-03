package at.helpch.papi.expansion;

import at.helpch.placeholderapi.PlaceholderAPI;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.chat.ChatUnlocked;
import net.milkbowl.vault2.helper.context.Context;
import net.milkbowl.vault2.helper.subject.Subject;
import net.milkbowl.vault2.permission.PermissionUnlocked;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class PermissionHook extends VaultHook {

    private PermissionUnlocked permission;
    private ChatUnlocked chat;

    public PermissionHook(VaultUnlockedExpansion expansion) {
        super(expansion);
        setup();
    }

    private @NotNull String[] getPlayerGroups(@NotNull final PlayerRef player, final boolean global) {
        return context(player, global)
                .map(context -> permission.getGroups(context, Subject.player(player.getUuid(), player.getUsername())))
                .orElse(new String[0]);
    }

    private @NotNull Optional<String> getPrimaryGroup(@NotNull final PlayerRef player, final boolean global) {
        return context(player, global)
                .map(context -> permission.primaryGroup(context, Subject.player(player.getUuid(), player.getUsername())));
    }

    private @NotNull Optional<String> getGroupMeta(@NotNull final PlayerRef player, @NotNull final String group,
                                                   final boolean isPrefix, final boolean global) {
        // No need to look up the meta if the group doesn't exist
        if (group.isEmpty()) {
            return Optional.empty();
        }

        return context(player, global)
                .flatMap(context -> {
                    final Subject subject = Subject.group(group);
                    return isPrefix ? chat.getPrefix(context, subject) : chat.getSuffix(context, subject);
                });
    }

    private @NotNull Optional<String> getPlayerMeta(@Nullable final PlayerRef player, final boolean isPrefix,
                                                    final boolean global) {
        return context(player, global)
                .flatMap(context -> {
                    final Subject subject = Subject.player(player.getUuid(), player.getUsername());

                    return isPrefix ? chat.getPrefix(context, subject) : chat.getSuffix(context, subject);
                });
    }

    @NotNull
    private Optional<Context> context(@Nullable final PlayerRef player, final boolean global) {
        if (player == null || global) {
            return Optional.of(Context.GLOBAL);
        }

        return Optional.ofNullable(player.getWorldUuid())
                .map(Universe.get()::getWorld)
                .map(World::getName)
                .map(Context::fromWorld);
    }

    private @NotNull String getGroupMeta(@NotNull final PlayerRef player, final int startIndex, final boolean isPrefix,
                                         final boolean global) {
        final String[] groups = getPlayerGroups(player, global);

        if (startIndex > groups.length) {
            return "";
        }

        for (int i = startIndex - 1; i < groups.length; i++) {
            final Optional<String> meta = getGroupMeta(player, groups[i], isPrefix, global);

            if (meta.isPresent()) {
                return meta.get();
            }
        }

        return "";
    }

    private @NotNull String capitalize(@NotNull final String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1).toLowerCase();
    }

    @Override
    public void setup() {
        permission = VaultUnlockedServicesManager.get().permissionObj();
        chat = VaultUnlockedServicesManager.get().chatObj();
    }

    @Override
    public boolean isReady() {
        return permission != null && chat != null;
    }

    @SuppressWarnings({"SpellCheckingInspection"})
    @Override
    public @Nullable String onRequest(@Nullable PlayerRef player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        final boolean global = params.startsWith("global_");

        if (global) {
            params = params.replace("global_", "");
        }

        if ((params.startsWith("rankprefix_") || params.startsWith("groupprefix_")) || (params.startsWith("ranksuffix_") || params.startsWith("groupsuffix_"))) {
            final String[] parts = params.split("_", 2);
            final Integer index = tryParse(parts[1]);

            if (index == null || index < 0) {
                return "Invalid number " + parts[1];
            }

            return getGroupMeta(player, index, parts[0].contains("prefix"), global);
        }

        if (params.startsWith("hasgroup_")) {
            final String group = params.substring("hasgroup_".length());

            return context(player, global)
                    .map(context -> permission.inGroup(context, Subject.player(player.getUuid(), player.getUsername()), group))
                    .map(PlaceholderAPI::booleanValue)
                    .orElse(PlaceholderAPI.booleanValue(false));
        }

        if (params.startsWith("inprimarygroup_")) {
            final String group = params.substring("inprimarygroup_".length());
            return getPrimaryGroup(player, global)
                    .map(group::equals)
                    .map(PlaceholderAPI::booleanValue)
                    .orElse(PlaceholderAPI.booleanValue(false));
        }

        return switch (params) {
            case "group", "rank" -> getPrimaryGroup(player, global).orElse("");
            case "group_capital", "rank_capital" -> getPrimaryGroup(player, global)
                    .map(this::capitalize)
                    .orElse("");
            case "groups", "ranks" -> String.join(", ", getPlayerGroups(player, global));
            case "groups_capital", "ranks_capital" -> Arrays.stream(getPlayerGroups(player, global))
                    .map(this::capitalize)
                    .collect(Collectors.joining(", "));
            case "prefix" -> getPlayerMeta(player, true, global).orElse("");
            case "suffix" -> getPlayerMeta(player, false, global).orElse("");
            case "groupprefix", "rankprefix" -> getPrimaryGroup(player, global)
                    .map(primaryGroup -> getGroupMeta(player, primaryGroup, true, global).orElse(""))
                    .orElse("");
            case "groupsuffix", "ranksuffix" -> getPrimaryGroup(player, global)
                    .map(primaryGroup -> getGroupMeta(player, primaryGroup, false, global).orElse(""))
                    .orElse("");
            default -> null;
        };
    }

}
