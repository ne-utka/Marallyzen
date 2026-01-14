package neutka.marallys.marallyzen.audio;

import net.neoforged.fml.ModList;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * Integration layer for PlasmoVoice mod.
 * Checks if PlasmoVoice is available and provides access to it.
 */
public final class VoiceIntegration {
    private static boolean available = false;
    private static boolean initialized = false;

    /**
     * Initializes the PlasmoVoice integration.
     * Should be called during mod initialization (e.g., in onCommonSetup).
     */
    public static void init() {
        if (initialized) {
            return;
        }
        
        boolean modLoaded = ModList.get().isLoaded("plasmovoice");
        
        // Also check if API classes are actually available
        if (modLoaded) {
            // Try multiple possible class names for PlasmoVoice API
            String[] possibleClassNames = {
                "su.plo.voice.api.server.VoiceServer",
                "su.plo.voice.api.server.PlasmoVoiceServer",
                "su.plo.voice.server.VoiceServer"
            };
            
            boolean apiFound = false;
            for (String className : possibleClassNames) {
                try {
                    Class.forName(className);
                    apiFound = true;
                    Marallyzen.LOGGER.info("PlasmoVoice API class found: {}", className);
                    break;
                } catch (ClassNotFoundException e) {
                    // Try next class name
                }
            }
            
            if (apiFound) {
                available = true;
                Marallyzen.LOGGER.info("PlasmoVoice integration enabled (mod loaded and API available)");
            } else {
                available = false;
                Marallyzen.LOGGER.warn("PlasmoVoice mod is loaded but API classes not found. Tried: {}", String.join(", ", possibleClassNames));
                Marallyzen.LOGGER.warn("This may be due to API version mismatch or mod not fully initialized. Using fallback.");
            }
        } else {
            available = false;
            Marallyzen.LOGGER.info("PlasmoVoice not found - audio features will use fallback");
        }
        
        initialized = true;
    }

    /**
     * Checks if PlasmoVoice is available.
     * @return true if PlasmoVoice mod is loaded, false otherwise
     */
    public static boolean isAvailable() {
        if (!initialized) {
            Marallyzen.LOGGER.warn("VoiceIntegration.init() was not called before checking availability");
            init(); // Auto-initialize as fallback
        }
        return available;
    }
}

