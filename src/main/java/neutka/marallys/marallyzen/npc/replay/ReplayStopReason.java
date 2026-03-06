package neutka.marallys.marallyzen.npc.replay;

/**
 * Public replay stop reason for admin inspection and diagnostics.
 */
public enum ReplayStopReason {
    MANUAL,
    FINISHED,
    DESYNC,
    NPC_REMOVED,
    SCRIPT_MISSING,
    COLLISION_STUCK,
    INVALID_STATE,
    ERROR,
    REPLACED,
    NONE
}
