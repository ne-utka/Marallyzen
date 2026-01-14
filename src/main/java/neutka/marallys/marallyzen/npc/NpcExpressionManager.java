package neutka.marallys.marallyzen.npc;

import net.minecraft.world.entity.Entity;

public final class NpcExpressionManager {
    public static final String EXPRESSION_IDLE = "idle";
    public static final String EXPRESSION_TALK = "talk";
    public static final String EXPRESSION_SMILE = "smile";
    public static final String EXPRESSION_ANGRY = "angry";
    public static final String EXPRESSION_SURPRISE = "surprise";

    private NpcExpressionManager() {
    }

    public static void applyDefaultExpression(Entity entity, NpcData data) {
        setExpression(entity, resolveDefaultExpression(data));
    }

    public static void applyTalkExpression(Entity entity, NpcData data) {
        setExpression(entity, resolveTalkExpression(data));
    }

    public static void setExpression(Entity entity, String expression) {
        if (!(entity instanceof GeckoNpcEntity geckoEntity)) {
            return;
        }
        if (expression == null || expression.isBlank()) {
            return;
        }
        geckoEntity.setExpression(expression);
    }

    public static String resolveDefaultExpression(NpcData data) {
        if (data != null && data.getGeckolibExpression() != null && !data.getGeckolibExpression().isBlank()) {
            return data.getGeckolibExpression();
        }
        return EXPRESSION_IDLE;
    }

    public static String resolveTalkExpression(NpcData data) {
        if (data != null && data.getGeckolibTalkExpression() != null && !data.getGeckolibTalkExpression().isBlank()) {
            return data.getGeckolibTalkExpression();
        }
        return EXPRESSION_TALK;
    }
}
