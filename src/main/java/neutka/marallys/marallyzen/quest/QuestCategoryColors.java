package neutka.marallys.marallyzen.quest;

public final class QuestCategoryColors {
    private QuestCategoryColors() {
    }

    public static int getColor(QuestCategory category) {
        if (category == null) {
            return 0xFFFFFF;
        }
        return switch (category) {
            case STORY -> 0xF2C94C;
            case EVENT -> 0xFF9F43;
            case SIDE -> 0x6FA8FF;
            case FARM -> 0x5DE0D8;
            case DAILY -> 0xA8E96A;
        };
    }
}
