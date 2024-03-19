package iudx.file.server.authenticator;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static iudx.file.server.authenticator.TokensForITs.*;

public class TokenSetup {
    private static final Logger logger = LoggerFactory.getLogger(TokenSetup.class);
    private static WebClient webClient;

    public static void setupTokens(String authEndpoint, String clientId, String clientSecret, String delegationId) {
        // Fetch tokens asynchronously and wait for all completions
        CompositeFuture.all(
                fetchToken("openResourceToken", authEndpoint, clientId, clientSecret),
                fetchToken("secureResourceToken", authEndpoint, clientId,clientSecret),
                fetchToken("adminToken", authEndpoint, clientId,clientSecret),
                fetchToken("delegateToken", authEndpoint, clientId,clientSecret, delegationId)

        ).onComplete(result -> {
            if (result.succeeded()) {
                logger.info("Tokens setup completed successfully");
                webClient.close();
            } else {
                // Handle failure, e.g., log the error
                logger.error("Error- {}", result.cause().getMessage());
                webClient.close();
            }
        });
    }

    private static Future<String> fetchToken(String userType, String authEndpoint, String clientID, String clientSecret) {
        Promise<String> promise = Promise.promise();
        JsonObject jsonPayload = getPayload(userType);
        // Create a WebClient to make the HTTP request
        webClient = WebClient.create(Vertx.vertx(), new WebClientOptions().setSsl(true));
        logger.info("Auth endpoint: {}", authEndpoint);
        webClient.postAbs(authEndpoint)
                .putHeader("Content-Type", "application/json")
                .putHeader("clientID", clientID)
                .putHeader("clientSecret", clientSecret)
                .sendJson(jsonPayload)
                .compose(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject jsonResponse = response.bodyAsJsonObject();
                        String accessToken = jsonResponse.getJsonObject("results").getString("accessToken");
                        // Store the token based on user type
                        switch (userType) {
                            case "secureResourceToken":
                                secureResourceToken = accessToken;
                                break;
                            case "openResourceToken":
                                openResourceToken = accessToken;
                                break;
                            case "adminToken":
                                adminToken = accessToken;
                        }
                        promise.complete(accessToken);
                    } else {
                        promise.fail("Failed to get token. Status code: " + response.statusCode());
                    }
                    return Future.succeededFuture();
                })
                .onFailure(throwable -> {
                    throwable.printStackTrace();
                    promise.fail(throwable);
                });
//                .onComplete(result -> {
//                    webClient.close();
//                });

        return promise.future();
    }

    private static Future<String> fetchToken(String userType, String authEndpoint, String clientID, String clientSecret, String delegationId) {
        Promise<String> promise = Promise.promise();
        JsonObject jsonPayload = getPayload(userType);
        // Create a WebClient to make the HTTP request for fetching delegate and adaptor tokens
        webClient = WebClient.create(Vertx.vertx(), new WebClientOptions().setSsl(true));

        webClient.postAbs(authEndpoint)
                .putHeader("Content-Type", "application/json")
                .putHeader("clientID", clientID)
                .putHeader("clientSecret", clientSecret)
                .putHeader("delegationId", delegationId)
                .sendJson(jsonPayload)
                .compose(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject jsonResponse = response.bodyAsJsonObject();
                        String accessToken = jsonResponse.getJsonObject("results").getString("accessToken");
                        // Store the token based on user type
                        if (userType.equals("delegateToken")) {
                            delegateToken = accessToken;
                        }
                        promise.complete(accessToken);
                    } else {
                        promise.fail("Failed to get token. Status code: " + response.statusCode());
                    }
                    return Future.succeededFuture();
                })
                .onFailure(throwable -> {
                    throwable.printStackTrace();
                    promise.fail(throwable);
                });
//                .onComplete(result -> {
//                    webClient.close();
//                });

        return promise.future();
    }

    @NotNull
    private static JsonObject getPayload(String userType) {
        JsonObject jsonPayload = new JsonObject();
        switch (userType) {
            case "openResourceToken":
                jsonPayload.put("itemId", "b58da193-23d9-43eb-b98a-a103d4b6103c");
                jsonPayload.put("itemType", "resource");
                jsonPayload.put("role", "consumer");
                break;
            case "secureResourceToken":
                jsonPayload.put("itemId", "83c2e5c2-3574-4e11-9530-2b1fbdfce832");
                jsonPayload.put("itemType", "resource");
                jsonPayload.put("role", "consumer");
                break;
            case "adminToken":
                jsonPayload.put("itemId", "rs.iudx.io");
                jsonPayload.put("itemType", "resource_server");
                jsonPayload.put("role", "admin");
                break;
            case "delegateToken":
                jsonPayload.put("itemId", "83c2e5c2-3574-4e11-9530-2b1fbdfce832");
                jsonPayload.put("itemType", "resource");
                jsonPayload.put("role", "delegate");
                break;
        }
        return jsonPayload;
    }

}
