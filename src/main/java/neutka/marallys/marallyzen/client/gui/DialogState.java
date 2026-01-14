package neutka.marallys.marallyzen.client.gui;

/**
 * Represents the state of a dialog in the state machine.
 * States follow this flow: IDLE → OPENING → CHOICE → EXECUTING → TRANSITION → CHOICE/CLOSED
 */
public enum DialogState {
    /**
     * Dialog is closed, nothing is rendered.
     */
    IDLE,
    
    /**
     * Smooth appearance animation (0.2-0.25 seconds).
     * Input is blocked during this state.
     */
    OPENING,
    
    /**
     * Dialog window is visible, player is choosing an option.
     */
    CHOICE,
    
    /**
     * Window is hidden, narration/animation/script is executing.
     */
    EXECUTING,
    
    /**
     * Pause between states (e.g., after narration completes, before opening next choice).
     */
    TRANSITION,
    
    /**
     * Dialog is completed and closed.
     */
    CLOSED
}


































