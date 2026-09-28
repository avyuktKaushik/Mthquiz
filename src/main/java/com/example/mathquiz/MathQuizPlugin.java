package com.example.mathquiz;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MathQuizPlugin extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String LINE = "<dark_gray><strikethrough>                                        ";

    private final AtomicReference<Question> current = new AtomicReference<>();
    private final QuestionGenerator generator = new QuestionGenerator();
    private RewardManager rewards;
    private BukkitTask autoTask;
    private BukkitTask expireTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        load();
        getLogger().info("MathQuiz enabled.");
    }

    @Override
    public void onDisable() {
        if (autoTask != null) autoTask.cancel();
        if (expireTask != null) expireTask.cancel();
    }

    private void load() {
        reloadConfig();
        generator.setDifficulty(getConfig().getString("difficulty", "normal"));
        generator.setAlgebraChance(getConfig().getInt("algebra-chance", 30));
        rewards = new RewardManager(getConfig());

        if (autoTask != null) autoTask.cancel();
        long interval = Math.max(10, getConfig().getLong("interval-seconds", 300)) * 20L;
        autoTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (Bukkit.getOnlinePlayers().size() >= getConfig().getInt("min-players", 1)) {
                askQuestion();
            }
        }, interval, interval);
    }

    /** Asks a new question. Returns false if one is already running. */
    private boolean askQuestion() {
        Question q = generator.generate();
        if (!current.compareAndSet(null, q)) return false;

        int seconds = getConfig().getInt("answer-time-seconds", 30);

        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(MM.deserialize(LINE));
        Bukkit.broadcast(MM.deserialize("  <gold><bold>✦ MATH QUIZ ✦"));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(MM.deserialize("  <yellow><bold>QUESTION <gray>- <white>" + q.prompt()));
        Bukkit.broadcast(MM.deserialize("  <aqua><bold>" + q.expression()));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(MM.deserialize("  <gray>First to type the answer in chat wins a <green>reward<gray>!"));
        Bukkit.broadcast(MM.deserialize("  <gray>You have <yellow>" + seconds + "s<gray>."));
        Bukkit.broadcast(MM.deserialize(LINE));
        Bukkit.broadcast(Component.empty());

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }

        expireTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (current.compareAndSet(q, null)) {
                Bukkit.broadcast(MM.deserialize(
                        "<red><bold>Time's up! <gray>Nobody got it. The answer was <yellow>" + q.answer()));
            }
        }, seconds * 20L);
        return true;
    }

    private Long parse(String raw) {
        String s = raw.trim().toLowerCase().replace(" ", "");
        if (s.startsWith("x=")) s = s.substring(2);
        if (s.isEmpty() || s.length() > 12) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Question q = current.get();
        if (q == null) return;

        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        Long guess = parse(text);
        if (guess == null || guess != q.answer()) return;

        // Only the very first correct answer gets through
        if (!current.compareAndSet(q, null)) return;

        event.setCancelled(true); // don't reveal the answer to others
        Player winner = event.getPlayer();

        // Give rewards on the main thread
        Bukkit.getScheduler().runTask(this, () -> {
            if (expireTask != null) expireTask.cancel();
            Component reward = rewards.giveRandomReward(winner);

            Bukkit.broadcast(Component.empty());
            Bukkit.broadcast(MM.deserialize(LINE));
            Bukkit.broadcast(MM.deserialize("  <green><bold>✔ CORRECT!"));
            Bukkit.broadcast(MM.deserialize("  <yellow>" + winner.getName()
                    + " <gray>solved it! Answer: <white>" + q.answer()));
            Bukkit.broadcast(MM.deserialize("  <gray>Reward: ").append(reward));
            Bukkit.broadcast(MM.deserialize(LINE));
            Bukkit.broadcast(Component.empty());

            winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("mathquiz.admin")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase() : "";
        switch (sub) {
            case "start" -> {
                if (!askQuestion()) sender.sendMessage(MM.deserialize("<red>A question is already active."));
            }
            case "stop" -> {
                if (current.getAndSet(null) != null) {
                    if (expireTask != null) expireTask.cancel();
                    sender.sendMessage(MM.deserialize("<yellow>Question cancelled."));
                } else {
                    sender.sendMessage(MM.deserialize("<red>No active question."));
                }
            }
            case "reload" -> {
                load();
                sender.sendMessage(MM.deserialize("<green>MathQuiz config reloaded."));
            }
            default -> sender.sendMessage(MM.deserialize("<gray>Usage: <yellow>/mathquiz <start|stop|reload>"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return args.length == 1 ? List.of("start", "stop", "reload") : List.of();
    }
}
