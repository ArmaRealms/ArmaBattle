package me.roinujnosde.titansbattle.managers;

import co.aikar.commands.BukkitLocales;
import co.aikar.commands.Locales;
import co.aikar.commands.PaperCommandManager;
import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.commands.ChallengeCommand;
import me.roinujnosde.titansbattle.commands.ConfigCommands;
import me.roinujnosde.titansbattle.commands.RankingCommand;
import me.roinujnosde.titansbattle.commands.TBCommands;
import me.roinujnosde.titansbattle.commands.completions.AbstractAsyncCompletion;
import me.roinujnosde.titansbattle.commands.completions.AbstractCompletion;
import me.roinujnosde.titansbattle.commands.completions.ArenasCompletion;
import me.roinujnosde.titansbattle.commands.completions.ChallengeCompletion;
import me.roinujnosde.titansbattle.commands.completions.ConfigFieldsCompletion;
import me.roinujnosde.titansbattle.commands.completions.DestinationCompletion;
import me.roinujnosde.titansbattle.commands.completions.GamesCompletion;
import me.roinujnosde.titansbattle.commands.completions.GroupsCompletion;
import me.roinujnosde.titansbattle.commands.completions.OrderByCompletion;
import me.roinujnosde.titansbattle.commands.completions.PagesCompletion;
import me.roinujnosde.titansbattle.commands.completions.PrizeCompletion;
import me.roinujnosde.titansbattle.commands.completions.RequestsCompletion;
import me.roinujnosde.titansbattle.commands.completions.WinnersDatesCompletion;
import me.roinujnosde.titansbattle.commands.conditions.AbstractCommandCondition;
import me.roinujnosde.titansbattle.commands.conditions.AbstractParameterCondition;
import me.roinujnosde.titansbattle.commands.conditions.ArenaReadyCondition;
import me.roinujnosde.titansbattle.commands.conditions.CanChallengeCondition;
import me.roinujnosde.titansbattle.commands.conditions.CanSpectateCondition;
import me.roinujnosde.titansbattle.commands.conditions.ChallengeCondition;
import me.roinujnosde.titansbattle.commands.conditions.EmptyInventoryCondition;
import me.roinujnosde.titansbattle.commands.conditions.GameReadyCondition;
import me.roinujnosde.titansbattle.commands.conditions.HappeningCondition;
import me.roinujnosde.titansbattle.commands.conditions.InvitedCondition;
import me.roinujnosde.titansbattle.commands.conditions.OtherGroupCondition;
import me.roinujnosde.titansbattle.commands.conditions.OtherPlayerCondition;
import me.roinujnosde.titansbattle.commands.conditions.ParticipantCondition;
import me.roinujnosde.titansbattle.commands.conditions.PlayerParticipantCondition;
import me.roinujnosde.titansbattle.commands.contexts.AbstractContextResolver;
import me.roinujnosde.titansbattle.commands.contexts.AbstractIssuerOnlyContextResolver;
import me.roinujnosde.titansbattle.commands.contexts.ArenaConfigurationContext;
import me.roinujnosde.titansbattle.commands.contexts.ChallengeContext;
import me.roinujnosde.titansbattle.commands.contexts.ChallengeRequestContext;
import me.roinujnosde.titansbattle.commands.contexts.DateContext;
import me.roinujnosde.titansbattle.commands.contexts.GameConfigurationContext;
import me.roinujnosde.titansbattle.commands.contexts.GameContext;
import me.roinujnosde.titansbattle.commands.contexts.GroupContext;
import me.roinujnosde.titansbattle.commands.contexts.OnlinePlayerContext;
import me.roinujnosde.titansbattle.commands.contexts.WarriorContext;
import me.roinujnosde.titansbattle.dao.ConfigurationDao;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public class CommandManager extends PaperCommandManager {

    public CommandManager(TitansBattle plugin) {
        super(plugin);
        enableUnstableAPI("help");
        setDefaultLocale();
        registerDependencies();
        registerCompletions();
        registerContexts();
        registerReplacements();
        registerConditions();
        registerCommands();
    }

    public static void dispatchCommand(@NotNull CommandSender sender, @NotNull String commandLine) {
        try {
            Bukkit.dispatchCommand(sender, commandLine);
        } catch (Exception exception) {
            TitansBattle.getInstance().getLogger().log(Level.SEVERE, String.format("Error executing command: %s", commandLine), exception);
        }
    }

    @Override
    public TitansBattle getPlugin() {
        return (TitansBattle) plugin;
    }

    private void setDefaultLocale() {
        BukkitLocales locales = getLocales();
        Locale defaultLocale = Locale.of(getPlugin().getConfigManager().getLanguage().split("_")[0]);
        locales.setDefaultLocale(defaultLocale);

        LanguageManager languageManager = getPlugin().getLanguageManager();
        locales.loadLanguage(languageManager.getEnglishLanguageFile(), Locales.ENGLISH);
        locales.loadLanguage(languageManager.getConfig(), defaultLocale);
    }

    private void registerDependencies() {
        registerDependency(GameManager.class, getPlugin().getGameManager());
        registerDependency(ConfigurationDao.class, getPlugin().getConfigurationDao());
        registerDependency(ConfigManager.class, getPlugin().getConfigManager());
        registerDependency(DatabaseManager.class, getPlugin().getDatabaseManager());
        registerDependency(ChallengeManager.class, getPlugin().getChallengeManager());
        registerDependency(TaskManager.class, getPlugin().getTaskManager());
        registerDependency(SpectateManager.class, getPlugin().getSpectateManager());
    }

    private void registerCompletions() {
        registerCompletion(new PagesCompletion(getPlugin()));
        registerCompletion(new GroupsCompletion(getPlugin()));
        registerCompletion(new RequestsCompletion(getPlugin()));
        registerCompletion(new ConfigFieldsCompletion(getPlugin()));
        registerCompletion(new OrderByCompletion(getPlugin()));
        registerCompletion(new GamesCompletion(getPlugin()));
        registerCompletion(new ArenasCompletion(getPlugin()));
        registerCompletion(new WinnersDatesCompletion(getPlugin()));
        registerCompletion(new DestinationCompletion(getPlugin()));
        registerCompletion(new PrizeCompletion(getPlugin()));
        registerCompletion(new ChallengeCompletion(getPlugin()));
    }

    private void registerContexts() {
        registerContext(new GameContext(getPlugin()));
        registerContext(new DateContext(getPlugin()));
        registerContext(new GroupContext(getPlugin()));
        registerContext(new WarriorContext(getPlugin()));
        registerContext(new ChallengeRequestContext(getPlugin()));
        registerContext(new ArenaConfigurationContext(getPlugin()));
        registerContext(new GameConfigurationContext(getPlugin()));
        registerContext(new OnlinePlayerContext(getPlugin()));
        registerContext(new ChallengeContext(getPlugin()));
    }

    private void registerReplacements() {
        ConfigurationSection commandsSection = getPlugin().getConfig().getConfigurationSection("commands");
        if (commandsSection == null) {
            return;
        }
        Set<String> commands = commandsSection.getKeys(false);
        for (String command : commands) {
            getCommandReplacements().addReplacement(command, commandsSection.getString(command) + "|" + command);
        }
    }

    private void registerConditions() {
        registerCondition(new PlayerParticipantCondition(getPlugin()));
        registerCondition(new ParticipantCondition(getPlugin()));
        registerCondition(new HappeningCondition(getPlugin()));
        registerCondition(new EmptyInventoryCondition(getPlugin()));
        registerCondition(new CanChallengeCondition(getPlugin()));
        registerCondition(new ArenaReadyCondition(getPlugin()));
        registerCondition(new GameReadyCondition(getPlugin()));
        registerCondition(new InvitedCondition(getPlugin()));
        registerCondition(new OtherPlayerCondition(getPlugin()));
        registerCondition(new OtherGroupCondition(getPlugin()));
        registerCondition(new ChallengeCondition(getPlugin()));
        registerCondition(new CanSpectateCondition(getPlugin()));
    }

    private void registerCommands() {
        registerCommand(new TBCommands());
        registerCommand(new RankingCommand());
        registerCommand(new ChallengeCommand());
        registerCommand(new ConfigCommands());
        registerCommand(new RankingCommand());
    }

    private <T> void registerContext(AbstractIssuerOnlyContextResolver<T> resolver) {
        getCommandContexts().registerIssuerOnlyContext(resolver.getType(), resolver);
    }

    private <T> void registerContext(AbstractContextResolver<T> resolver) {
        getCommandContexts().registerContext(resolver.getType(), resolver);
    }

    private void registerCompletion(AbstractCompletion completion) {
        getCommandCompletions().registerCompletion(completion.getId(), completion);
    }

    private void registerCompletion(AbstractAsyncCompletion completion) {
        getCommandCompletions().registerAsyncCompletion(completion.getId(), completion);
    }

    private void registerCondition(AbstractCommandCondition condition) {
        getCommandConditions().addCondition(condition.getId(), condition);
    }

    private <T> void registerCondition(AbstractParameterCondition<T> condition) {
        getCommandConditions().addCondition(condition.getType(), condition.getId(), condition);
    }

}
