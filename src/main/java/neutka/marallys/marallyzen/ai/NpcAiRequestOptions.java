package neutka.marallys.marallyzen.ai;

public record NpcAiRequestOptions(
        String model,
        Double temperature,
        Integer maxTokens
) { }
