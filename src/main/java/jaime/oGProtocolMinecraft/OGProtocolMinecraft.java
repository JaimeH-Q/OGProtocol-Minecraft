package jaime.oGProtocolMinecraft;

import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class OGProtocolMinecraft extends JavaPlugin {

    public static String BACKEND_BASE_URL = "http://129.151.100.83:25594";




    @Override
    public void onEnable() {
        registerListeners();

        getCommand("login").setExecutor(new MainCmd(this));

    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MainListener(this), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }





    private final Set<String> loggedPlayers = new HashSet<>();

    public static boolean checkValidSession(String name, String ip) {
        try {
            // Construir JSON del body
            String jsonBody = String.format("{\"username\":\"%s\",\"ip\":\"%s\"}", name, ip);

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OGProtocolMinecraft.BACKEND_BASE_URL + "/validatesession"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Ejecutar de manera asíncrona
            CompletableFuture<Boolean> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        int status = response.statusCode();
                        // 200 significa sesión válida
                        return status == 200;
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return false;
                    });

            // Bloquear hasta recibir respuesta y retornar boolean
            return future.get();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Set<String> getLoggedPlayers() {
        return loggedPlayers;
    }

    public void addLoggedPlayer(String name){
        loggedPlayers.add(name);
    }
}
