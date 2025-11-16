package jaime.oGProtocolMinecraft;

import io.papermc.paper.event.player.PlayerSignCommandPreprocessEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static jaime.oGProtocolMinecraft.MessageUtils.colorize;
import static jaime.oGProtocolMinecraft.OGProtocolMinecraft.checkValidSession;

public class MainListener implements Listener {

    private static final Logger log = LoggerFactory.getLogger(MainListener.class);
    private final OGProtocolMinecraft plugin;


    public MainListener(OGProtocolMinecraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        plugin.getLoggedPlayers().remove(event.getPlayer().getName());
        plugin.getLogger().log(Level.ALL, "Player " + event.getPlayer().getName() + " removed from logged sessions");
    }

    @EventHandler
    public void onPlayerPreConnect(AsyncPlayerPreLoginEvent event){
        String name = event.getPlayerProfile().getName();
        String ip = event.getRawAddress().getHostAddress();
        if(checkValidSession(name, ip)){
            plugin.getLoggedPlayers().add(event.getName());
            plugin.getLogger().log(Level.ALL, "Player " + event.getName() + " had a valid previous session, logging him");

        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getLoggedPlayers().contains(player.getName())) {
            // Solo cancelar si cambió de posición (x, y, z), ignorando la rotación
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onPlayerConnect(PlayerJoinEvent event) {
        if (plugin.getLoggedPlayers().contains(event.getPlayer().getName())) {
            event.getPlayer().sendMessage(colorize("&aWelcome back! Auto logged-in"));
            return;
        }
        event.getPlayer().sendMessage(colorize("&eWelcome! Use /login to login"));
    }

}
