package jaime.oGProtocolMinecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.bukkit.map.MapPalette;


import static jaime.oGProtocolMinecraft.MessageUtils.colorize;
import static jaime.oGProtocolMinecraft.OGProtocolMinecraft.checkValidSession;

public class MainCmd implements CommandExecutor {

    private final OGProtocolMinecraft plugin;

    public MainCmd(OGProtocolMinecraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(!(sender instanceof Player player)) return true;

        player.sendMessage(colorize("&eGenerating login link..."));


        generateLoginLinkAsync(player, link -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (link == null) {
                    player.sendMessage(colorize("&cError generating login link. Try again."));
                } else {
                    net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text("Login here: ")
                            .append(net.kyori.adventure.text.Component.text(link)
                                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(link))
                                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                            net.kyori.adventure.text.Component.text("Click to open link")
                                    ))
                                    .color(net.kyori.adventure.text.format.TextColor.color(0x00FF00)) // verde
                            );

                    try {
                        BufferedImage qr = generateQRCode(link, 128);
                        giveQRMap(player, qr);

                    } catch (WriterException e) {
                        player.sendMessage(colorize("&cWe couldn't generate the QR map for you"));
                    }
                    player.sendMessage(message);
                    startLoginTask(player);
                }
            });
        });

        return false;
    }

    private void startLoginTask(Player player) {
        new BukkitRunnable() {
            int attempts = 0;
            final int MAX_ATTEMPTS = 15;

            @Override
            public void run() {
                attempts++;

                if (checkValidSession(player.getName(), player.getAddress().getHostName())) {
                    plugin.addLoggedPlayer(player.getName());
                    player.sendTitle(
                            "§aSuccessfully logged in!", // Título
                            "",                          // Subtítulo vacío
                            10,                          // Fade-in ticks
                            70,                          // Stay ticks
                            20                           // Fade-out ticks
                    );
                    cancel();
                    clearMapsFromInventory(player);
                    return;
                }

                if (attempts >= MAX_ATTEMPTS) {
                    cancel();
                    clearMapsFromInventory(player);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 40L); // 20L = 1 segundo entre intentos
    }


    private void generateLoginLinkAsync(Player player, Consumer<String> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String url = "http://129.151.100.83:25594/token?username=" +
                        URLEncoder.encode(player.getName(), StandardCharsets.UTF_8);

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    callback.accept(null);
                    return;
                }

                String body = response.body();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String token = json.get("token").getAsString();

                callback.accept("http://129.151.100.83:25595/login?token=" + token + "&username=" + player.getName() + "&ip=" + player.getAddress().getHostName());

            } catch (Exception e) {
                e.printStackTrace();
                callback.accept(null);
            }
        });
    }

    public BufferedImage generateQRCode(String text, int size) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size);

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int color = bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF;
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    public void giveQRMap(Player player, BufferedImage qrImage) {
        MapView map = player.getServer().createMap(player.getWorld());
        map.getRenderers().clear();

        map.addRenderer(new MapRenderer() {
            private boolean rendered = false;

            @Override
            public void render(MapView mapView, MapCanvas canvas, Player player) {
                if (rendered) return;

                int mapSize = 128; // mapa estándar
                for (int x = 0; x < mapSize; x++) {
                    for (int y = 0; y < mapSize; y++) {
                        int qrX = x * qrImage.getWidth() / mapSize;
                        int qrY = y * qrImage.getHeight() / mapSize;

                        int rgb = qrImage.getRGB(qrX, qrY);
                        Color color = new Color(rgb);

                        // Convertir al byte que entiende el mapa
                        byte mapColor = MapPalette.matchColor(color);
                        canvas.setPixel(x, y, mapColor);
                    }
                }

                rendered = true;
            }
        });

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
        mapMeta.setMapView(map);
        mapItem.setItemMeta(mapMeta);
        player.getInventory().addItem(mapItem);
    }

    public void clearMapsFromInventory(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.FILLED_MAP) {
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
    }
}
