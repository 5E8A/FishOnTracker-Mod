package fishontracker.client.scan;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardReader {

    private static final Pattern WALLET_PATTERN = Pattern.compile("\\$\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([kKmMbBtT]?)");

    public static long getWalletBalance() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return 0;

        try {
            Scoreboard scoreboard = client.level.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

            if (objective == null) return 0;

            Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective);

            for (PlayerScoreEntry score : scores) {
                Component displayComp = score.display();
                String lineText = displayComp != null ? displayComp.getString() : score.owner();

                PlayerTeam team = scoreboard.getPlayersTeam(score.owner());
                if (team != null) {
                    String prefix = team.getPlayerPrefix().getString();
                    String suffix = team.getPlayerSuffix().getString();
                    lineText = prefix + lineText + suffix;
                }

                String cleanLine = lineText.replaceAll("§[0-9a-fk-or]", "")
                                           .replaceAll("&[0-9a-fk-or]", "")
                                           .trim();

                long balance = parseWalletValue(cleanLine);
                if (balance > 0) {
                    return balance;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static long parseWalletValue(String text) {
        try {
            Matcher matcher = WALLET_PATTERN.matcher(text);

            if (matcher.find()) {
                double number = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2).toUpperCase();

                switch (unit) {
                    case "K": number *= 1_000; break;
                    case "M": number *= 1_000_000; break;
                    case "B": number *= 1_000_000_000; break;
                    case "T": number *= 1_000_000_000_000L; break;
                    default: break;
                }

                return (long) number;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}