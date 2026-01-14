package neutka.marallys.marallyzen.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * State machine for managing dialog states and transitions.
 * Manages timers for automatic state transitions and notifies subscribers of state changes.
 */
public class DialogStateMachine {
    private static DialogStateMachine instance;
    
    private DialogState currentState = DialogState.IDLE;
    private int stateTimer = 0; // Timer in ticks for automatic transitions
    private DialogState pendingTransition = null; // State to transition to after timer expires
    
    // Subscribers that are notified when state changes
    private final List<Consumer<DialogState>> stateChangeListeners = new ArrayList<>();
    
    // Timing constants (in ticks, 20 ticks = 1 second)
    private static final int OPENING_DURATION_TICKS = 5; // 0.25 seconds
    private static final int TRANSITION_DURATION_TICKS = 3; // 0.15 seconds
    
    public static DialogStateMachine getInstance() {
        if (instance == null) {
            instance = new DialogStateMachine();
        }
        return instance;
    }
    
    private DialogStateMachine() {
    }
    
    /**
     * Gets the current state of the dialog.
     */
    public DialogState getCurrentState() {
        return currentState;
    }
    
    /**
     * Transitions to a new state immediately.
     * If the state requires a timer, it will be set up automatically.
     */
    public void transitionTo(DialogState newState) {
        if (currentState == newState) {
            return; // Already in this state
        }
        
        DialogState oldState = currentState;
        currentState = newState;
        stateTimer = 0;
        pendingTransition = null;
        
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogStateMachine: Transitioning from {} to {}", oldState, newState);
        
        // Set up automatic transitions based on state
        switch (newState) {
            case OPENING:
                // After opening animation, transition to CHOICE
                stateTimer = OPENING_DURATION_TICKS;
                pendingTransition = DialogState.CHOICE;
                neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("DialogStateMachine: OPENING state - will transition to CHOICE in {} ticks", stateTimer);
                break;
            case TRANSITION:
                // After transition pause, we wait for external signal (from server)
                // No automatic transition here
                break;
            case CLOSED:
                // After closed, transition to IDLE
                stateTimer = 1; // Immediate transition
                pendingTransition = DialogState.IDLE;
                break;
            default:
                // Other states don't have automatic transitions
                break;
        }
        
        // Notify all subscribers
        notifyStateChange(newState, oldState);
    }
    
    /**
     * Transitions to a new state after a delay (in ticks).
     */
    public void transitionToAfter(DialogState newState, int delayTicks) {
        if (delayTicks <= 0) {
            transitionTo(newState);
            return;
        }
        
        stateTimer = delayTicks;
        pendingTransition = newState;
    }
    
    /**
     * Updates the state machine. Should be called every tick.
     */
    public void tick() {
        if (pendingTransition != null && stateTimer > 0) {
            stateTimer--;
            if (stateTimer <= 0) {
                DialogState targetState = pendingTransition;
                pendingTransition = null;
                transitionTo(targetState);
            }
        }
    }
    
    /**
     * Checks if input should be blocked in the current state.
     */
    public boolean isInputBlocked() {
        return currentState == DialogState.OPENING || 
               currentState == DialogState.EXECUTING || 
               currentState == DialogState.TRANSITION;
    }
    
    /**
     * Checks if the dialog window should be visible in the current state.
     */
    public boolean shouldShowDialogWindow() {
        return currentState == DialogState.OPENING || 
               currentState == DialogState.CHOICE;
    }
    
    /**
     * Adds a listener that will be notified when the state changes.
     */
    public void addStateChangeListener(Consumer<DialogState> listener) {
        stateChangeListeners.add(listener);
    }
    
    /**
     * Removes a state change listener.
     */
    public void removeStateChangeListener(Consumer<DialogState> listener) {
        stateChangeListeners.remove(listener);
    }
    
    /**
     * Notifies all subscribers of a state change.
     */
    private void notifyStateChange(DialogState newState, DialogState oldState) {
        for (Consumer<DialogState> listener : stateChangeListeners) {
            try {
                listener.accept(newState);
            } catch (Exception e) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.error("Error in dialog state change listener", e);
            }
        }
    }
    
    /**
     * Resets the state machine to IDLE state.
     */
    public void reset() {
        transitionTo(DialogState.IDLE);
    }
}

