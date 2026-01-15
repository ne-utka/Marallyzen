package neutka.marallys.marallyzen.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import neutka.marallys.marallyzen.Marallyzen;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NpcAiService {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ScheduledExecutorService RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final int MAX_RETRIES = 2;

    public record ChatMessage(String role, String content) { }
    public record AiResponse(String reply, List<String> options) { }

    public static CompletableFuture<AiResponse> generateResponse(List<ChatMessage> messages, NpcAiRequestOptions options) {
        NpcAiGlobalConfig global = NpcAiGlobalConfig.get();
        if (!global.enabled) {
            return CompletableFuture.failedFuture(new IllegalStateException("AI is disabled in config"));
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", options.model() != null && !options.model().isBlank() ? options.model() : global.model);
        body.addProperty("temperature", options.temperature() != null ? options.temperature() : global.temperature);
        body.addProperty("max_tokens", options.maxTokens() != null ? options.maxTokens() : global.maxTokens);
        if (global.baseUrl != null && global.baseUrl.contains("openai.com")) {
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_object");
            body.add("response_format", responseFormat);
        }

        JsonArray msgArray = new JsonArray();
        for (ChatMessage message : messages) {
            JsonObject msg = new JsonObject();
            msg.addProperty("role", message.role());
            msg.addProperty("content", message.content());
            msgArray.add(msg);
        }
        body.add("messages", msgArray);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(global.baseUrl))
                .timeout(Duration.ofSeconds(global.timeoutSeconds))
                .header("Content-Type", "application/json");

        if (global.apiKey != null && !global.apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + global.apiKey);
        }

        HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        return sendWithRetry(request, 0);
    }

    private static CompletableFuture<AiResponse> sendWithRetry(HttpRequest request, int attempt) {
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    int status = response.statusCode();
                    if (status == 429 && attempt < MAX_RETRIES) {
                        long delaySeconds = parseRetryAfterSeconds(response).orElse(20L);
                        CompletableFuture<AiResponse> delayed = new CompletableFuture<>();
                        RETRY_EXECUTOR.schedule(
                                () -> sendWithRetry(request, attempt + 1).whenComplete((res, err) -> {
                                    if (err != null) {
                                        delayed.completeExceptionally(err);
                                    } else {
                                        delayed.complete(res);
                                    }
                                }),
                                delaySeconds,
                                TimeUnit.SECONDS
                        );
                        return delayed;
                    }
                    if (status < 200 || status >= 300) {
                        throw new IllegalStateException("AI response status " + status + ": " + response.body());
                    }
                    return CompletableFuture.completedFuture(parseResponse(response.body()));
                });
    }

    private static java.util.OptionalLong parseRetryAfterSeconds(HttpResponse<String> response) {
        try {
            String value = response.headers().firstValue("Retry-After").orElse(null);
            if (value == null || value.isBlank()) {
                return java.util.OptionalLong.empty();
            }
            long seconds = Long.parseLong(value.trim());
            return java.util.OptionalLong.of(Math.max(1, seconds));
        } catch (Exception e) {
            return java.util.OptionalLong.empty();
        }
    }

    private static AiResponse parseResponse(String responseBody) {
        JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("AI response missing choices");
        }
        JsonObject first = choices.get(0).getAsJsonObject();
        JsonObject message = first.getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            throw new IllegalStateException("AI response missing message content");
        }
        String content = message.get("content").getAsString();
        if (content == null) {
            throw new IllegalStateException("AI response content is null");
        }
        JsonObject json = extractJsonObject(content);
        if (json == null) {
            return new AiResponse(stripFences(content).trim(), List.of());
        }
        String reply = json.has("reply") ? json.get("reply").getAsString() : "";
        List<String> options = new ArrayList<>();
        if (json.has("options") && json.get("options").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("options")) {
                if (!element.isJsonNull()) {
                    String option = element.getAsString().trim();
                    if (!option.isEmpty()) {
                        options.add(option);
                    }
                }
            }
        }
        return new AiResponse(reply, options);
    }

    private static JsonObject extractJsonObject(String content) {
        String trimmed = stripFences(content);
        try {
            return GSON.fromJson(trimmed, JsonObject.class);
        } catch (Exception ignored) {
            // Continue to substring extraction.
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String slice = trimmed.substring(start, end + 1);
            try {
                return GSON.fromJson(slice, JsonObject.class);
            } catch (Exception e) {
                Marallyzen.LOGGER.warn("Failed to parse AI JSON slice: {}", slice, e);
            }
        }
        return null;
    }

    private static String stripFences(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int fenceEnd = trimmed.lastIndexOf("```");
            if (fenceEnd >= 0) {
                trimmed = trimmed.substring(0, fenceEnd);
            }
        }
        return trimmed.trim();
    }
}
