package jaime.oGProtocolMinecraft;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private static final Pattern colorPattern = Pattern.compile("(?<!\\\\)(#([a-fA-F0-9]{6}))");

    public static String colorize(String s) {
        s = s.replaceAll("&#", "#");
        Matcher matcher = colorPattern.matcher(s);
        StringBuilder buffer = new StringBuilder();
        int lastAppendPosition = 0;

        while (matcher.find()) {
            buffer.append(s, lastAppendPosition, matcher.start());

            String colorCode = matcher.group(1);
            ChatColor chatColor = ChatColor.of(colorCode);

            if (chatColor != null) {
                buffer.append(chatColor);
            } else {
                buffer.append(matcher.group(0)); // Append the original text if ChatColor is null
            }

            lastAppendPosition = matcher.end();
        }

        buffer.append(s, lastAppendPosition, s.length());

        s = buffer.toString();
        return ChatColor.translateAlternateColorCodes('&', s);
    }


}
