package neutka.marallys.marallyzen.npc;

import neutka.marallys.marallyzen.DenizenService;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads dialog options from Denizen scripts.
 * Parses assignment and interact scripts to extract dialog choices.
 */
public class DialogScriptLoader {
    
    public record DialogOptions(
            Map<String, String> texts, 
            Map<String, String> emotes,
            Map<String, List<String>> expressions,
            Map<String, List<String>> narrateMessages, // Map: optionId -> list of narrate messages
            Map<String, Integer> narrateDurations,     // Map: optionId -> duration in ticks
            int defaultDuration,                        // Default duration for all messages if not specified per case
            Map<String, Integer> nextSteps,             // Map: optionId -> next step number (0 = no next step)
            Map<String, DialogOptions> nestedDialogs,   // Map: caseId -> nested dialog options from nested choose blocks
            Map<String, ScreenFadeData> screenFades,    // Map: optionId -> screen fade data
            Map<String, EyesCloseData> eyesCloses,      // Map: optionId -> eyes close cutscene data
            Map<String, ItemEquipData> itemEquips,      // Map: optionId -> item equip data (mainhand/offhand)
            Map<String, AudioData> audioData,           // Map: optionId -> audio playback data (single audio, deprecated)
            Map<String, List<AudioData>> audioDataList  // Map: optionId -> list of audio playback data (multiple audio support)
    ) { }
    
    /**
     * Data class for screen fade cutscene parameters.
     */
    public record ScreenFadeData(
            int fadeOutTicks,
            int blackScreenTicks,
            int fadeInTicks,
            String titleText,
            String subtitleText,
            boolean blockPlayerInput,
            String soundId  // ResourceLocation ID of sound (e.g., "minecraft:block.anvil.land")
    ) { }
    
    /**
     * Data class for eyes close cutscene parameters.
     */
    public record EyesCloseData(
            int closeDurationTicks,
            int blackDurationTicks,
            int openDurationTicks,
            boolean lockPlayer
    ) { }
    
    /**
     * Data class for item equip parameters.
     */
    public record ItemEquipData(
            String itemId,      // Item ID (e.g., "marallyzen:locator")
            String hand         // "mainhand" or "offhand"
    ) { }
    
    /**
     * Data class for audio playback parameters.
     */
    public record AudioData(
            String filePath,    // Path to audio file (relative to config/marallyzen/audio/)
            String source,      // "npc", "player", or position string
            boolean positional, // Whether audio is positional or global
            float radius,       // Sound radius for positional audio
            boolean blocking,  // Whether to block script execution until audio finishes
            String narration    // Optional narration text to show with audio
    ) { }
    
    private static final Map<String, DialogOptions> dialogCache = new HashMap<>();
    
    /**
     * Loads dialog options for a given dialog script name and step number.
     * @param dialogScriptName The name of the dialog script (e.g., "merchant_dialog")
     * @param stepNumber The step number to load (1 = first step, default)
     * @return DialogOptions (texts + emote ids), or null if script not found
     */
    public static DialogOptions loadDialogOptions(String dialogScriptName, int stepNumber) {
        if (dialogScriptName == null || dialogScriptName.isEmpty()) {
            return null;
        }
        
        // Check cache first (cache key includes step number)
        String cacheKey = dialogScriptName + "_step_" + stepNumber;
        if (dialogCache.containsKey(cacheKey)) {
            DialogOptions cached = dialogCache.get(cacheKey);
            Map<String, DialogOptions> nestedDialogsCopy = new LinkedHashMap<>();
            if (cached.nestedDialogs() != null) {
                for (Map.Entry<String, DialogOptions> entry : cached.nestedDialogs().entrySet()) {
                    nestedDialogsCopy.put(entry.getKey(), entry.getValue());
                }
            }
            return new DialogOptions(
                    new LinkedHashMap<>(cached.texts()), 
                    new LinkedHashMap<>(cached.emotes()),
                    new LinkedHashMap<>(cached.expressions()),
                    new LinkedHashMap<>(cached.narrateMessages()),
                    new LinkedHashMap<>(cached.narrateDurations()),
                    cached.defaultDuration(),
                    new LinkedHashMap<>(cached.nextSteps()),
                    nestedDialogsCopy,
                    new LinkedHashMap<>(cached.screenFades()),
                    new LinkedHashMap<>(cached.eyesCloses()),
                    new LinkedHashMap<>(cached.itemEquips()),
                    new LinkedHashMap<>(cached.audioData()),
                    cached.audioDataList() != null ? new LinkedHashMap<>(cached.audioDataList()) : new LinkedHashMap<>()
            );
        }
        
        try {
            File scriptsFolder = DenizenService.getScriptsFolder();
            File scriptFile = new File(scriptsFolder, dialogScriptName + ".dsc");
            
            if (!scriptFile.exists()) {
                Marallyzen.LOGGER.warn("Dialog script file not found: {}", scriptFile.getAbsolutePath());
                return null;
            }
            
            // Read file content
            String content = readFile(scriptFile);
            
            // Parse options for specific step using simple text parsing (to avoid YAML dependency issues)
            DialogOptions optionData = parseDialogOptions(content, stepNumber);
            
            if (optionData == null || optionData.texts().isEmpty()) {
                Marallyzen.LOGGER.warn("Failed to parse dialog options from script '{}'", dialogScriptName);
                return null;
            }
            
            // Cache the result
            Map<String, DialogOptions> nestedDialogsCopy = new LinkedHashMap<>();
            if (optionData.nestedDialogs() != null) {
                for (Map.Entry<String, DialogOptions> entry : optionData.nestedDialogs().entrySet()) {
                    nestedDialogsCopy.put(entry.getKey(), entry.getValue());
                }
            }
            dialogCache.put(cacheKey, new DialogOptions(
                    new LinkedHashMap<>(optionData.texts()), 
                    new LinkedHashMap<>(optionData.emotes()),
                    new LinkedHashMap<>(optionData.expressions()),
                    new LinkedHashMap<>(optionData.narrateMessages()),
                    new LinkedHashMap<>(optionData.narrateDurations()),
                    optionData.defaultDuration(),
                    new LinkedHashMap<>(optionData.nextSteps()),
                    nestedDialogsCopy,
                    new LinkedHashMap<>(optionData.screenFades()),
                    new LinkedHashMap<>(optionData.eyesCloses()),
                    new LinkedHashMap<>(optionData.itemEquips()),
                    new LinkedHashMap<>(optionData.audioData()),
                    optionData.audioDataList() != null ? new LinkedHashMap<>(optionData.audioDataList()) : new LinkedHashMap<>()
            ));
            
            return optionData;
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to load dialog options from script '{}'", dialogScriptName, e);
            return null;
        }
    }
    
    /**
     * Loads dialog options for a given dialog script name (defaults to step 1).
     * @param dialogScriptName The name of the dialog script (e.g., "merchant_dialog")
     * @return DialogOptions (texts + emote ids), or null if script not found
     */
    public static DialogOptions loadDialogOptions(String dialogScriptName) {
        return loadDialogOptions(dialogScriptName, 1);
    }
    
    /**
     * Parses dialog options from script content for a specific step using simple text parsing.
     */
    private static DialogOptions parseDialogOptions(String content, int stepNumber) {
        Map<String, String> optionTexts = new LinkedHashMap<>();
        Map<String, String> optionEmotes = new LinkedHashMap<>();
        Map<String, List<String>> optionExpressions = new LinkedHashMap<>();
        Map<String, List<String>> narrateMessages = new LinkedHashMap<>();
        Map<String, Integer> narrateDurations = new LinkedHashMap<>();
        Map<String, Integer> nextSteps = new LinkedHashMap<>();
        Map<String, DialogOptions> nestedDialogs = new LinkedHashMap<>();
        Map<String, ScreenFadeData> screenFades = new LinkedHashMap<>();
        Map<String, EyesCloseData> eyesCloses = new LinkedHashMap<>();
        Map<String, ItemEquipData> itemEquips = new LinkedHashMap<>();
        Map<String, AudioData> audioData = new LinkedHashMap<>();
        Map<String, List<AudioData>> audioDataList = new LinkedHashMap<>(); // For multiple audio support
        
        // Find the specific step block
        Pattern stepPattern = Pattern.compile("\\s+" + stepNumber + "\\s*:\\s*\\n(.*?)(?=\\s+\\d+\\s*:|\\s+requirements:|\\Z)", Pattern.DOTALL);
        Matcher stepMatcher = stepPattern.matcher(content);
        
        if (!stepMatcher.find()) {
            // Step not found, return empty options
            return new DialogOptions(optionTexts, optionEmotes, optionExpressions, narrateMessages, narrateDurations, 100, nextSteps, nestedDialogs, screenFades, eyesCloses, itemEquips, audioData, audioDataList);
        }
        
        String stepContent = stepMatcher.group(1);
        
        // Parse default duration from step level (before case blocks)
        // Look for: define duration:<[3s]> at the step level
        int defaultDuration = parseNarrateDuration(stepContent);
        if (defaultDuration == 0) {
            defaultDuration = 100; // Default 100 ticks (5 seconds) if not specified in script
        }
        
        // Find the "define options:<list[...]>" line in this step
        Pattern optionsPattern = Pattern.compile("define\\s+options:\\s*<list\\[([^\\]]+)\\]>");
        Matcher optionsMatcher = optionsPattern.matcher(stepContent);
        
        List<String> options = null;
        if (optionsMatcher.find()) {
            String listContent = optionsMatcher.group(1);
            options = Arrays.asList(listContent.split("\\|"));
        }
        
        if (options == null || options.isEmpty()) {
            return new DialogOptions(optionTexts, optionEmotes, optionExpressions, narrateMessages, narrateDurations, defaultDuration, nextSteps, nestedDialogs, screenFades, eyesCloses, itemEquips, audioData, audioDataList);
        }
        
        // Optional: define emotes:<list[emote1|emote2|...]>
        Pattern emotesPattern = Pattern.compile("define\\s+emotes:\\s*<list\\[([^\\]]+)\\]>");
        Matcher emotesMatcher = emotesPattern.matcher(stepContent);
        List<String> emoteList = null;
        if (emotesMatcher.find()) {
            String listContent = emotesMatcher.group(1);
            emoteList = Arrays.asList(listContent.split("\\|"));
        }

        // Optional: define expressions:<list[expression1|expression2|...]>
        Pattern expressionsPattern = Pattern.compile("define\\s+expressions:\\s*<list\\[([^\\]]+)\\]>");
        Matcher expressionsMatcher = expressionsPattern.matcher(stepContent);
        List<String> stepExpressions = null;
        if (expressionsMatcher.find()) {
            String listContent = expressionsMatcher.group(1);
            stepExpressions = Arrays.asList(listContent.split("\\|"));
        }
        
        // Find the first "choose" block to get only top-level cases
        // We only want cases from the first choose statement, not nested ones
        Pattern choosePattern = Pattern.compile("choose\\s*<\\[options\\]>\\s*:");
        Matcher chooseMatcher = choosePattern.matcher(stepContent);
        
        String casesContent = stepContent;
        int chooseStartOffset = 0;
        int expectedCaseIndent = -1;
        int firstChooseIndent = -1;
        
        if (chooseMatcher.find()) {
            // Save the indentation of the first choose for later use
            String beforeFirstChoose = stepContent.substring(0, chooseMatcher.start());
            int firstChooseLastNewline = beforeFirstChoose.lastIndexOf('\n');
            String firstChooseLine = stepContent.substring(firstChooseLastNewline + 1, chooseMatcher.start());
            firstChooseIndent = firstChooseLine.length() - firstChooseLine.replaceAll("^\\s+", "").length();
            // First-level cases should be indented one level more than "choose" (typically +4 spaces)
            expectedCaseIndent = firstChooseIndent + 4;
            
            // Extract content from first "choose" until next "choose" or end of step
            chooseStartOffset = chooseMatcher.end();
            // Find next "choose" at same or higher indentation level, or end of step
            Pattern nextChoosePattern = Pattern.compile("\\n\\s+-\\s+choose");
            Matcher nextChooseMatcher = nextChoosePattern.matcher(stepContent);
            int chooseEnd = stepContent.length();
            if (nextChooseMatcher.find(chooseStartOffset)) {
                // Check indentation level by counting leading spaces before "choose"
                int nextChoosePos = nextChooseMatcher.start();
                String beforeNextChoose = stepContent.substring(0, nextChoosePos);
                int lastNewline = beforeNextChoose.lastIndexOf('\n');
                String nextChooseLine = stepContent.substring(lastNewline + 1, nextChoosePos);
                int nextChooseIndent = nextChooseLine.length() - nextChooseLine.replaceAll("^\\s+", "").length();
                
                // Only use next choose if it's at same or less indentation (same level or parent)
                if (nextChooseIndent <= firstChooseIndent) {
                    chooseEnd = nextChooseMatcher.start();
                }
            }
            casesContent = stepContent.substring(chooseStartOffset, chooseEnd);
        }
        
        // Find all "case" statements only in the first-level choose block
        // Support case names with spaces, special characters, and Russian text
        // Only match cases at the same indentation level (directly after "choose")
        // We need to check indentation to ensure we only get first-level cases
        
        // Find all case statements and filter by indentation
        Pattern casePattern = Pattern.compile("^(\\s+)-\\s+case\\s+([^:]+?)\\s*:", Pattern.MULTILINE);
        Matcher caseMatcher = casePattern.matcher(casesContent);
        
        Set<String> foundCases = new HashSet<>();
        List<CaseMatch> caseMatches = new ArrayList<>();
        while (caseMatcher.find()) {
            String indentStr = caseMatcher.group(1);
            int caseIndent = indentStr.length();
            String caseId = caseMatcher.group(2).trim(); // Trim whitespace from case ID
            
            // Only include cases at the expected indentation level (first level)
            // Allow some tolerance (±2 spaces) for formatting variations
            if (expectedCaseIndent < 0 || Math.abs(caseIndent - expectedCaseIndent) <= 2) {
                foundCases.add(caseId);
                optionTexts.put(caseId, formatOptionText(caseId));
                // Store position relative to stepContent
                int absoluteStart = chooseStartOffset + caseMatcher.start();
                int absoluteEnd = chooseStartOffset + caseMatcher.end();
                caseMatches.add(new CaseMatch(caseId, absoluteStart, absoluteEnd));
                Marallyzen.LOGGER.debug("DialogScriptLoader: Found case '{}' in step {} (first level, indent={})", caseId, stepNumber, caseIndent);
            } else {
                Marallyzen.LOGGER.debug("DialogScriptLoader: Skipping nested case '{}' (indent={}, expected={})", caseId, caseIndent, expectedCaseIndent);
            }
        }
        
        // Parse narrate messages, durations, and next steps for each case
        // Only parse content for first-level cases
        for (int i = 0; i < caseMatches.size(); i++) {
            CaseMatch currentCase = caseMatches.get(i);
            int startPos = currentCase.end();
            // Find end position: next first-level case or end of casesContent
            int endPos;
            if (i < caseMatches.size() - 1) {
                endPos = caseMatches.get(i + 1).start();
            } else {
                // Last case: end at end of casesContent (which is relative to stepContent)
                endPos = chooseStartOffset + casesContent.length();
            }
            
            String caseContent = stepContent.substring(startPos, endPos);
            List<String> messages = parseNarrateMessages(caseContent);
            List<String> expressions = parseExpressionsList(caseContent);
            int duration = parseNarrateDuration(caseContent);
            int nextStep = parseNextStep(caseContent);
            ScreenFadeData screenFade = parseScreenFade(caseContent);
            EyesCloseData eyesClose = parseEyesClose(caseContent);
            ItemEquipData itemEquip = parseItemEquip(caseContent);
            List<AudioData> audioList = parseAudioCommands(caseContent);
            AudioData audio = audioList.isEmpty() ? null : audioList.get(0); // For backward compatibility
            
            // Check for nested choose block in this case
            DialogOptions nestedDialog = parseNestedChooseBlock(caseContent, defaultDuration);
            if (nestedDialog != null && !nestedDialog.texts().isEmpty()) {
                nestedDialogs.put(currentCase.caseId(), nestedDialog);
                Marallyzen.LOGGER.info("DialogScriptLoader: Found nested dialog for case '{}' with {} options", 
                        currentCase.caseId(), nestedDialog.texts().size());
            }
            
            Marallyzen.LOGGER.info("DialogScriptLoader: Parsed case '{}': messages={}, duration={}, nextStep={}, hasNested={}", 
                    currentCase.caseId(), messages.size(), duration, nextStep, nestedDialog != null && !nestedDialog.texts().isEmpty());
            
            if (!messages.isEmpty()) {
                narrateMessages.put(currentCase.caseId(), messages);
                // Use case-specific duration if provided, otherwise use default
                narrateDurations.put(currentCase.caseId(), duration > 0 ? duration : defaultDuration);
                Marallyzen.LOGGER.info("DialogScriptLoader: Added narrate messages for case '{}': {}", 
                        currentCase.caseId(), messages);
            } else {
                Marallyzen.LOGGER.warn("DialogScriptLoader: Case '{}' has no narrate messages", currentCase.caseId());
            }
            if (expressions != null && !expressions.isEmpty()) {
                optionExpressions.put(currentCase.caseId(), expressions);
                Marallyzen.LOGGER.info("DialogScriptLoader: Added expressions for case '{}': {}", 
                        currentCase.caseId(), expressions);
            }
            if (nextStep > 0) {
                nextSteps.put(currentCase.caseId(), nextStep);
                Marallyzen.LOGGER.info("DialogScriptLoader: Case '{}' has next step {}", currentCase.caseId(), nextStep);
            }
            if (screenFade != null) {
                screenFades.put(currentCase.caseId(), screenFade);
                Marallyzen.LOGGER.info("DialogScriptLoader: Case '{}' has screen fade: fadeOut={}t, blackScreen={}t, fadeIn={}t, title='{}'", 
                        currentCase.caseId(), screenFade.fadeOutTicks(), screenFade.blackScreenTicks(), screenFade.fadeInTicks(), screenFade.titleText());
            }
            if (eyesClose != null) {
                eyesCloses.put(currentCase.caseId(), eyesClose);
                Marallyzen.LOGGER.info("DialogScriptLoader: Case '{}' has eyes close: close={}t, black={}t, open={}t, lock={}", 
                        currentCase.caseId(), eyesClose.closeDurationTicks(), eyesClose.blackDurationTicks(), eyesClose.openDurationTicks(), eyesClose.lockPlayer());
            }
            if (itemEquip != null) {
                itemEquips.put(currentCase.caseId(), itemEquip);
                Marallyzen.LOGGER.info("DialogScriptLoader: Case '{}' has item equip: item={}, hand={}", 
                        currentCase.caseId(), itemEquip.itemId(), itemEquip.hand());
            }
            if (!audioList.isEmpty()) {
                // Store first audio for backward compatibility
                audioData.put(currentCase.caseId(), audioList.get(0));
                // Store all audio files for sequential playback
                audioDataList.put(currentCase.caseId(), audioList);
                Marallyzen.LOGGER.info("DialogScriptLoader: Case '{}' has {} audio file(s): {}", 
                        currentCase.caseId(), audioList.size(), 
                        audioList.stream().map(AudioData::filePath).toList());
            }
        }
        
        // If we found options but no cases, create default texts for all options
        if (optionTexts.isEmpty()) {
            for (String option : options) {
                optionTexts.put(option.trim(), formatOptionText(option.trim()));
            }
        } else {
            // Add any options that don't have cases
            for (String option : options) {
                String trimmed = option.trim();
                if (!optionTexts.containsKey(trimmed)) {
                    optionTexts.put(trimmed, formatOptionText(trimmed));
                }
            }
        }

        // Attach emote ids by order if provided
        if (emoteList != null && !emoteList.isEmpty()) {
            for (int i = 0; i < options.size() && i < emoteList.size(); i++) {
                String optId = options.get(i).trim();
                String emoteId = emoteList.get(i).trim();
                if (!emoteId.isEmpty()) {
                    optionEmotes.put(optId, emoteId);
                }
            }
        }

        // Attach expressions by order if provided and not overridden in case content
        if (stepExpressions != null && !stepExpressions.isEmpty()) {
            for (int i = 0; i < options.size() && i < stepExpressions.size(); i++) {
                String optId = options.get(i).trim();
                if (optionExpressions.containsKey(optId)) {
                    continue;
                }
                String expressionId = stepExpressions.get(i).trim();
                if (!expressionId.isEmpty()) {
                    optionExpressions.put(optId, java.util.List.of(expressionId));
                }
            }
        }
        
        return new DialogOptions(optionTexts, optionEmotes, optionExpressions, narrateMessages, narrateDurations, defaultDuration, nextSteps, nestedDialogs, screenFades, eyesCloses, itemEquips, audioData, audioDataList);
    }
    
    /**
     * Helper class to store case match information.
     */
    private static class CaseMatch {
        final String caseId;
        final int start;
        final int end;
        
        CaseMatch(String caseId, int start, int end) {
            this.caseId = caseId;
            this.start = start;
            this.end = end;
        }
        
        String caseId() { return caseId; }
        int start() { return start; }
        int end() { return end; }
    }
    
    /**
     * Parses narrate messages from case content.
     * Looks for lines like: - narrate "message"
     */
    private static List<String> parseNarrateMessages(String caseContent) {
        List<String> messages = new ArrayList<>();
        Pattern narratePattern = Pattern.compile("-\\s+narrate\\s+\"([^\"]+)\"");
        Matcher narrateMatcher = narratePattern.matcher(caseContent);
        while (narrateMatcher.find()) {
            messages.add(narrateMatcher.group(1));
        }
        return messages;
    }
    
    /**
     * Parses screenfade command from case content.
     * Looks for lines like: - screenfade fade_out:1s black_screen:5s fade_in:1s title:"..." subtitle:"..." block_input:false
     * 
     * @param caseContent The case content to parse
     * @return ScreenFadeData if found, null otherwise
     */
    private static ScreenFadeData parseScreenFade(String caseContent) {
        // Pattern to match screenfade command with all parameters
        // Example: - screenfade fade_out:1s black_screen:5s fade_in:1s title:"30 минут спустя" subtitle:"11 августа 2024 г." block_input:false
        Pattern screenFadePattern = Pattern.compile("-\\s+screenfade\\s+(.*?)(?=\\n|$)");
        Matcher matcher = screenFadePattern.matcher(caseContent);
        
        if (!matcher.find()) {
            return null;
        }
        
        String paramsStr = matcher.group(1);
        
        // Parse individual parameters
        int fadeOutTicks = 20; // Default: 1 second
        int blackScreenTicks = 100; // Default: 5 seconds
        int fadeInTicks = 20; // Default: 1 second
        String titleText = null;
        String subtitleText = null;
        boolean blockPlayerInput = false;
        String soundId = null;
        
        // Parse fade_out
        Pattern fadeOutPattern = Pattern.compile("fade_out:([^\\s]+)");
        Matcher fadeOutMatcher = fadeOutPattern.matcher(paramsStr);
        if (fadeOutMatcher.find()) {
            fadeOutTicks = parseDurationToTicks(fadeOutMatcher.group(1));
        }
        
        // Parse black_screen
        Pattern blackScreenPattern = Pattern.compile("black_screen:([^\\s]+)");
        Matcher blackScreenMatcher = blackScreenPattern.matcher(paramsStr);
        if (blackScreenMatcher.find()) {
            blackScreenTicks = parseDurationToTicks(blackScreenMatcher.group(1));
        }
        
        // Parse fade_in
        Pattern fadeInPattern = Pattern.compile("fade_in:([^\\s]+)");
        Matcher fadeInMatcher = fadeInPattern.matcher(paramsStr);
        if (fadeInMatcher.find()) {
            fadeInTicks = parseDurationToTicks(fadeInMatcher.group(1));
        }
        
        // Parse title (can contain spaces, so match quoted string)
        Pattern titlePattern = Pattern.compile("title:\"([^\"]+)\"");
        Matcher titleMatcher = titlePattern.matcher(paramsStr);
        if (titleMatcher.find()) {
            titleText = titleMatcher.group(1);
        }
        
        // Parse subtitle (can contain spaces, so match quoted string)
        Pattern subtitlePattern = Pattern.compile("subtitle:\"([^\"]+)\"");
        Matcher subtitleMatcher = subtitlePattern.matcher(paramsStr);
        if (subtitleMatcher.find()) {
            subtitleText = subtitleMatcher.group(1);
        }
        
        // Parse block_input
        Pattern blockInputPattern = Pattern.compile("block_input:(true|false)");
        Matcher blockInputMatcher = blockInputPattern.matcher(paramsStr);
        if (blockInputMatcher.find()) {
            blockPlayerInput = Boolean.parseBoolean(blockInputMatcher.group(1));
        }
        
        // Parse sound (can be quoted or unquoted ResourceLocation)
        // Pattern matches: sound:"minecraft:block.anvil.land" or sound:minecraft:block.anvil.land
        Pattern soundPattern = Pattern.compile("sound:(?:\"([^\"]+)\"|([^\\s]+))");
        Matcher soundMatcher = soundPattern.matcher(paramsStr);
        if (soundMatcher.find()) {
            soundId = soundMatcher.group(1) != null ? soundMatcher.group(1) : soundMatcher.group(2);
        }
        
        return new ScreenFadeData(fadeOutTicks, blackScreenTicks, fadeInTicks, titleText, subtitleText, blockPlayerInput, soundId);
    }
    
    /**
     * Parses eyescutscene command from case content.
     * Looks for lines like: - eyescutscene close_duration:1s black_duration:5s open_duration:1s lock_player:true
     * 
     * @param caseContent The case content to parse
     * @return EyesCloseData if found, null otherwise
     */
    private static EyesCloseData parseEyesClose(String caseContent) {
        // Pattern to match eyescutscene command with all parameters
        Pattern eyesClosePattern = Pattern.compile("-\\s+eyescutscene\\s+(.*?)(?=\\n|$)");
        Matcher matcher = eyesClosePattern.matcher(caseContent);
        
        if (!matcher.find()) {
            return null;
        }
        
        String paramsStr = matcher.group(1);
        
        // Default values
        int closeDurationTicks = 20;  // 1 second
        int blackDurationTicks = 100; // 5 seconds
        int openDurationTicks = 20;   // 1 second
        boolean lockPlayer = true;
        
        // Parse close_duration
        Pattern closePattern = Pattern.compile("close_duration:(\\d+[st]?)");
        Matcher closeMatcher = closePattern.matcher(paramsStr);
        if (closeMatcher.find()) {
            closeDurationTicks = parseDurationToTicks(closeMatcher.group(1));
        }
        
        // Parse black_duration
        Pattern blackPattern = Pattern.compile("black_duration:(\\d+[st]?)");
        Matcher blackMatcher = blackPattern.matcher(paramsStr);
        if (blackMatcher.find()) {
            blackDurationTicks = parseDurationToTicks(blackMatcher.group(1));
        }
        
        // Parse open_duration
        Pattern openPattern = Pattern.compile("open_duration:(\\d+[st]?)");
        Matcher openMatcher = openPattern.matcher(paramsStr);
        if (openMatcher.find()) {
            openDurationTicks = parseDurationToTicks(openMatcher.group(1));
        }
        
        // Parse lock_player
        Pattern lockPattern = Pattern.compile("lock_player:(true|false)");
        Matcher lockMatcher = lockPattern.matcher(paramsStr);
        if (lockMatcher.find()) {
            lockPlayer = Boolean.parseBoolean(lockMatcher.group(1));
        }
        
        Marallyzen.LOGGER.info("DialogScriptLoader: Parsed eyescutscene - close={}t, black={}t, open={}t, lock={}", 
                closeDurationTicks, blackDurationTicks, openDurationTicks, lockPlayer);
        
        return new EyesCloseData(closeDurationTicks, blackDurationTicks, openDurationTicks, lockPlayer);
    }
    
    /**
     * Parses giveitem command from case content.
     * Looks for lines like: - giveitem marallyzen:locator mainhand
     * or: - giveitem marallyzen:locator offhand
     * 
     * @param caseContent The case content to parse
     * @return ItemEquipData if found, null otherwise
     */
    private static ItemEquipData parseItemEquip(String caseContent) {
        // Pattern to match giveitem command: - giveitem <item_id> <hand>
        // Example: - giveitem marallyzen:locator mainhand
        Pattern giveItemPattern = Pattern.compile("-\\s+giveitem\\s+([^\\s]+)\\s+(mainhand|offhand)");
        Matcher matcher = giveItemPattern.matcher(caseContent);
        
        if (!matcher.find()) {
            return null;
        }
        
        String itemId = matcher.group(1);
        String hand = matcher.group(2);
        
        return new ItemEquipData(itemId, hand);
    }
    
    /**
     * Parses all play_audio commands from case content.
     * Looks for lines like: - play_audio file:"dialogues/npc_guard_warn.ogg" source:npc mode:positional radius:10 blocking:true narration:"Стой! Дальше нельзя."
     * 
     * @param caseContent The case content to parse
     * @return List of AudioData (can be empty if no audio commands found)
     */
    private static List<AudioData> parseAudioCommands(String caseContent) {
        List<AudioData> audioList = new ArrayList<>();
        
        // Debug: log case content to see what we're parsing
        Marallyzen.LOGGER.debug("DialogScriptLoader: parseAudioCommands - caseContent length={}, content:\n{}", caseContent.length(), caseContent);
        
        // Pattern to match play_audio command with all parameters
        // Example: - play_audio file:"dialogues/npc_guard_warn.ogg" source:npc mode:positional radius:10 blocking:true narration:"Стой! Дальше нельзя."
        // Use DOTALL mode to match across newlines, and make it non-greedy until next command (starts with -)
        Pattern playAudioPattern = Pattern.compile("-\\s+play_audio\\s+(.*?)(?=\\n\\s*-|$)", Pattern.DOTALL);
        Matcher matcher = playAudioPattern.matcher(caseContent);
        
        // Find all play_audio commands in the case
        while (matcher.find()) {
            String paramsStr = matcher.group(1);
            
            // Default values for this audio command
            String filePath = null;
            String source = "npc";
            boolean positional = true;
            float radius = 32.0f; // Increased default radius for better NPC audio range
            boolean blocking = false;
            String narration = null;
            
            // Parse file (quoted string)
            Pattern filePattern = Pattern.compile("file:\"([^\"]+)\"");
            Matcher fileMatcher = filePattern.matcher(paramsStr);
            if (fileMatcher.find()) {
                filePath = fileMatcher.group(1);
            }
            
            if (filePath == null) {
                // Try unquoted file path
                Pattern filePatternUnquoted = Pattern.compile("file:([^\\s]+)");
                Matcher fileMatcherUnquoted = filePatternUnquoted.matcher(paramsStr);
                if (fileMatcherUnquoted.find()) {
                    filePath = fileMatcherUnquoted.group(1);
                }
            }
            
            if (filePath == null) {
                continue; // Skip this audio command if file is missing
            }
            
            // Parse source (npc, player, or position)
            Pattern sourcePattern = Pattern.compile("source:([^\\s]+)");
            Matcher sourceMatcher = sourcePattern.matcher(paramsStr);
            if (sourceMatcher.find()) {
                source = sourceMatcher.group(1);
            }
            
            // Parse mode (positional or global)
            Pattern modePattern = Pattern.compile("mode:(positional|global)");
            Matcher modeMatcher = modePattern.matcher(paramsStr);
            if (modeMatcher.find()) {
                positional = modeMatcher.group(1).equals("positional");
            }
            
            // Parse radius
            Pattern radiusPattern = Pattern.compile("radius:([\\d.]+)");
            Matcher radiusMatcher = radiusPattern.matcher(paramsStr);
            if (radiusMatcher.find()) {
                try {
                    radius = Float.parseFloat(radiusMatcher.group(1));
                } catch (NumberFormatException e) {
                    // Keep default
                }
            }
            
            // Parse blocking
            Pattern blockingPattern = Pattern.compile("blocking:(true|false)");
            Matcher blockingMatcher = blockingPattern.matcher(paramsStr);
            if (blockingMatcher.find()) {
                blocking = Boolean.parseBoolean(blockingMatcher.group(1));
            }
            
            // Parse narration (quoted string, optional)
            Pattern narrationPattern = Pattern.compile("narration:\"([^\"]+)\"");
            Matcher narrationMatcher = narrationPattern.matcher(paramsStr);
            if (narrationMatcher.find()) {
                narration = narrationMatcher.group(1);
            }
            
            audioList.add(new AudioData(filePath, source, positional, radius, blocking, narration));
        }
        
        return audioList;
    }
    
    /**
     * Parses a duration string to ticks.
     * Supports formats: "1s" (seconds), "20t" (ticks), or plain number (ticks).
     */
    private static int parseDurationToTicks(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            return 20; // Default: 1 second
        }
        
        durationStr = durationStr.trim().toLowerCase();
        
        // Check for seconds format: "1s", "5s", etc.
        if (durationStr.endsWith("s")) {
            try {
                int seconds = Integer.parseInt(durationStr.substring(0, durationStr.length() - 1));
                return seconds * 20; // Convert seconds to ticks
            } catch (NumberFormatException e) {
                return 20;
            }
        }
        
        // Check for ticks format: "20t", "100t", etc.
        if (durationStr.endsWith("t")) {
            try {
                return Integer.parseInt(durationStr.substring(0, durationStr.length() - 1));
            } catch (NumberFormatException e) {
                return 20;
            }
        }
        
        // Plain number (assumed to be ticks)
        try {
            return Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            return 20;
        }
    }
    
    /**
     * Parses initial screen fade from step content (before choose statement).
     * These are screen fades that should be shown when dialog first opens (after initial narration).
     * 
     * @param dialogScriptName The name of the dialog script
     * @param stepNumber The step number (usually 1 for initial dialog)
     * @return ScreenFadeData if found, null otherwise
     */
    public static ScreenFadeData parseInitialScreenFade(String dialogScriptName, int stepNumber) {
        try {
            File scriptsFolder = DenizenService.getScriptsFolder();
            File scriptFile = new File(scriptsFolder, dialogScriptName + ".dsc");
            
            if (!scriptFile.exists()) {
                return null;
            }
            
            String content = readFile(scriptFile);
            
            // Find the specific step block
            Pattern stepPattern = Pattern.compile("\\s+" + stepNumber + "\\s*:\\s*\\n(.*?)(?=\\s+\\d+\\s*:|\\s+requirements:|\\Z)", Pattern.DOTALL);
            Matcher stepMatcher = stepPattern.matcher(content);
            
            if (!stepMatcher.find()) {
                return null;
            }
            
            String stepContent = stepMatcher.group(1);
            
            // Find the click trigger block
            Pattern clickTriggerPattern = Pattern.compile("click\\s+trigger:\\s*\\n\\s*script:\\s*\\n(.*?)(?=\\s+-\\s+define|\\s+-\\s+choose|\\Z)", Pattern.DOTALL);
            Matcher clickTriggerMatcher = clickTriggerPattern.matcher(stepContent);
            
            if (!clickTriggerMatcher.find()) {
                // Try alternative pattern
                clickTriggerPattern = Pattern.compile("click\\s+trigger:.*?script:.*?\\n(.*?)(?=\\s+-\\s+define|\\s+-\\s+choose|\\Z)", Pattern.DOTALL);
                clickTriggerMatcher = clickTriggerPattern.matcher(stepContent);
            }
            
            if (!clickTriggerMatcher.find()) {
                return null;
            }
            
            String scriptContent = clickTriggerMatcher.group(1);
            
            // Extract content before the first "define options" or "choose" statement
            Pattern stopPattern = Pattern.compile("(define\\s+options|choose)");
            Matcher stopMatcher = stopPattern.matcher(scriptContent);
            int stopIndex = scriptContent.length();
            if (stopMatcher.find()) {
                stopIndex = stopMatcher.start();
            }
            
            String initialContent = scriptContent.substring(0, stopIndex);
            
            // Parse screenfade from initial content
            return parseScreenFade(initialContent);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to parse initial screen fade for script '{}' step {}", dialogScriptName, stepNumber, e);
            return null;
        }
    }
    
    /**
     * Parses initial narrate messages and duration from step content (before choose statement).
     * These are messages that should be shown when dialog first opens.
     * 
     * @param dialogScriptName The name of the dialog script
     * @param stepNumber The step number (usually 1 for initial dialog)
     * @return Pair of (messages list, duration in ticks), or (empty list, 0) if none found
     */
    public static java.util.Map.Entry<List<String>, Integer> parseInitialNarrateMessages(String dialogScriptName, int stepNumber) {
        try {
            File scriptsFolder = DenizenService.getScriptsFolder();
            File scriptFile = new File(scriptsFolder, dialogScriptName + ".dsc");
            
            if (!scriptFile.exists()) {
                return new java.util.AbstractMap.SimpleEntry<>(new ArrayList<>(), 0);
            }
            
            String content = readFile(scriptFile);
            
            // Find the specific step block
            Pattern stepPattern = Pattern.compile("\\s+" + stepNumber + "\\s*:\\s*\\n(.*?)(?=\\s+\\d+\\s*:|\\s+requirements:|\\Z)", Pattern.DOTALL);
            Matcher stepMatcher = stepPattern.matcher(content);
            
            if (!stepMatcher.find()) {
                return new java.util.AbstractMap.SimpleEntry<>(new ArrayList<>(), 0);
            }
            
            String stepContent = stepMatcher.group(1);
            
            // Parse duration from step level (before case blocks)
            int duration = parseNarrateDuration(stepContent);
            if (duration == 0) {
                duration = 100; // Default 100 ticks (5 seconds) if not specified
            }
            
            String scriptContent = extractInitialScriptContent(stepContent);
            if (scriptContent == null) {
                Marallyzen.LOGGER.warn("DialogScriptLoader: No click trigger block found in step {} for script '{}'", stepNumber, dialogScriptName);
                Marallyzen.LOGGER.debug("DialogScriptLoader: Step content preview: {}", stepContent.substring(0, Math.min(200, stepContent.length())));
                return new java.util.AbstractMap.SimpleEntry<>(new ArrayList<>(), duration);
            }
            Marallyzen.LOGGER.debug("DialogScriptLoader: Found click trigger block, content length: {}, preview: {}", 
                    scriptContent.length(), scriptContent.substring(0, Math.min(100, scriptContent.length())));
            
            List<String> messages = parseNarrateMessages(scriptContent);
            Marallyzen.LOGGER.info("DialogScriptLoader: Parsed {} initial narrate messages from step {}: {}", 
                    messages.size(), stepNumber, messages);
            
            // Also check for duration in initial content (case-specific duration)
            int initialDuration = parseNarrateDuration(scriptContent);
            if (initialDuration > 0) {
                duration = initialDuration;
                Marallyzen.LOGGER.debug("DialogScriptLoader: Found duration {} ticks in initial content", initialDuration);
            }
            
            return new java.util.AbstractMap.SimpleEntry<>(messages, duration);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to parse initial narrate messages for script '{}' step {}", dialogScriptName, stepNumber, e);
            return new java.util.AbstractMap.SimpleEntry<>(new ArrayList<>(), 0);
        }
    }

    public static List<AudioData> parseInitialAudioCommands(String dialogScriptName, int stepNumber) {
        try {
            File scriptsFolder = DenizenService.getScriptsFolder();
            File scriptFile = new File(scriptsFolder, dialogScriptName + ".dsc");

            if (!scriptFile.exists()) {
                return new ArrayList<>();
            }

            String content = readFile(scriptFile);

            Pattern stepPattern = Pattern.compile("\\s+" + stepNumber + "\\s*:\\s*\\n(.*?)(?=\\s+\\d+\\s*:|\\s+requirements:|\\Z)", Pattern.DOTALL);
            Matcher stepMatcher = stepPattern.matcher(content);

            if (!stepMatcher.find()) {
                return new ArrayList<>();
            }

            String stepContent = stepMatcher.group(1);
            String scriptContent = extractInitialScriptContent(stepContent);
            if (scriptContent == null) {
                return new ArrayList<>();
            }
            return parseAudioCommands(scriptContent);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to parse initial audio commands for script '{}' step {}", dialogScriptName, stepNumber, e);
            return new ArrayList<>();
        }
    }

    public static String parseInitialNarratorName(String dialogScriptName, int stepNumber) {
        try {
            File scriptsFolder = DenizenService.getScriptsFolder();
            File scriptFile = new File(scriptsFolder, dialogScriptName + ".dsc");

            if (!scriptFile.exists()) {
                return null;
            }

            String content = readFile(scriptFile);

            Pattern stepPattern = Pattern.compile("\\s+" + stepNumber + "\\s*:\\s*\\n(.*?)(?=\\s+\\d+\\s*:|\\s+requirements:|\\Z)", Pattern.DOTALL);
            Matcher stepMatcher = stepPattern.matcher(content);

            if (!stepMatcher.find()) {
                return null;
            }

            String stepContent = stepMatcher.group(1);
            String scriptContent = extractInitialScriptContent(stepContent);
            if (scriptContent == null) {
                return null;
            }
            
            Pattern namePattern = Pattern.compile("(?:-\\s+)?define\\s+(?:name|narrator|speaker):\\s*<\\[([^\\]]+)\\]>");
            Matcher nameMatcher = namePattern.matcher(scriptContent);
            if (nameMatcher.find()) {
                return nameMatcher.group(1).trim();
            }
            
            Pattern nameQuotedPattern = Pattern.compile("(?:-\\s+)?define\\s+(?:name|narrator|speaker):\\s*\"([^\"]+)\"");
            Matcher nameQuotedMatcher = nameQuotedPattern.matcher(scriptContent);
            if (nameQuotedMatcher.find()) {
                return nameQuotedMatcher.group(1).trim();
            }

            return null;
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to parse initial narrator name for script '{}' step {}", dialogScriptName, stepNumber, e);
            return null;
        }
    }

    private static String extractInitialScriptContent(String stepContent) {
        String[] lines = stepContent.split("\\r?\\n");
        int clickIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equalsIgnoreCase("click trigger:")) {
                clickIndex = i;
                break;
            }
        }
        if (clickIndex < 0) {
            return null;
        }
        int scriptIndex = -1;
        for (int i = clickIndex + 1; i < lines.length; i++) {
            if (lines[i].trim().equalsIgnoreCase("script:")) {
                scriptIndex = i;
                break;
            }
        }
        if (scriptIndex < 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = scriptIndex + 1; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("- ")) {
                builder.append(lines[i]).append('\n');
            } else if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                break;
            }
        }
        String content = builder.toString();
        if (content.isEmpty()) {
            return null;
        }
        Pattern stopPattern = Pattern.compile("(define\\s+options|choose)");
        Matcher stopMatcher = stopPattern.matcher(content);
        int stopIndex = content.length();
        if (stopMatcher.find()) {
            stopIndex = stopMatcher.start();
        }
        return content.substring(0, stopIndex);
    }
    
    /**
     * Parses narrate duration from content (can be script-level or case-level).
     * Looks for lines like: - define duration:<[3s]> or - define duration:<[60]>
     * Returns duration in ticks (1 second = 20 ticks).
     * Returns 0 if not found.
     */
    private static int parseNarrateDuration(String content) {
        // Try to find: define duration:<[3s]> or define duration:<[60]>
        // Match both with and without leading "- " (for script-level vs case-level)
        Pattern durationPattern = Pattern.compile("(?:-\\s+)?define\\s+duration:\\s*<\\[([0-9]+)([sm]?)\\]>");
        Matcher durationMatcher = durationPattern.matcher(content);
        if (durationMatcher.find()) {
            int value = Integer.parseInt(durationMatcher.group(1));
            String unit = durationMatcher.group(2);
            if (unit.equals("s")) {
                return value * 20; // Convert seconds to ticks
            } else {
                return value; // Already in ticks
            }
        }
        return 0; // No duration specified
    }

    private static List<String> parseExpressionsList(String content) {
        Pattern expressionsPattern = Pattern.compile("define\\s+expressions:\\s*<list\\[([^\\]]+)\\]>");
        Matcher expressionsMatcher = expressionsPattern.matcher(content);
        if (!expressionsMatcher.find()) {
            return java.util.Collections.emptyList();
        }
        String listContent = expressionsMatcher.group(1);
        if (listContent == null || listContent.isBlank()) {
            return java.util.Collections.emptyList();
        }
        String[] parts = listContent.split("\\|");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
    
    /**
     * Parses nested choose block from case content.
     * Looks for a "choose <[options]>:" block after narration messages.
     * Returns DialogOptions for the nested dialog, or null if not found.
     */
    private static DialogOptions parseNestedChooseBlock(String caseContent, int defaultDuration) {
        // Find nested "choose <[options]>:" block
        // It should appear after narration messages and define statements
        Pattern choosePattern = Pattern.compile("choose\\s*<\\[options\\]>\\s*:");
        Matcher chooseMatcher = choosePattern.matcher(caseContent);
        
        if (!chooseMatcher.find()) {
            return null; // No nested choose block
        }
        
        // Get the indentation of the nested choose to determine boundaries
        int chooseStart = chooseMatcher.start();
        String beforeChoose = caseContent.substring(0, chooseStart);
        int lastNewline = beforeChoose.lastIndexOf('\n');
        String chooseLine = caseContent.substring(lastNewline + 1, chooseStart);
        int chooseIndent = chooseLine.length() - chooseLine.replaceAll("^\\s+", "").length();
        
        // Extract content from nested choose until next choose at same or less indentation, or end
        int chooseContentStart = chooseMatcher.end();
        int chooseContentEnd = caseContent.length();
        
        // Find next choose at same or less indentation
        Pattern nextChoosePattern = Pattern.compile("\\n(\\s+)-\\s+choose");
        Matcher nextChooseMatcher = nextChoosePattern.matcher(caseContent);
        if (nextChooseMatcher.find(chooseContentStart)) {
            String nextChooseIndentStr = nextChooseMatcher.group(1);
            int nextChooseIndent = nextChooseIndentStr.length();
            if (nextChooseIndent <= chooseIndent) {
                chooseContentEnd = nextChooseMatcher.start();
            }
        }
        
        String nestedChooseContent = caseContent.substring(chooseContentStart, chooseContentEnd);
        
        // Parse options from define options:<list[...]>
        Pattern optionsPattern = Pattern.compile("define\\s+options:\\s*<list\\[([^\\]]+)\\]>");
        Matcher optionsMatcher = optionsPattern.matcher(nestedChooseContent);
        
        List<String> options = null;
        if (optionsMatcher.find()) {
            String listContent = optionsMatcher.group(1);
            options = Arrays.asList(listContent.split("\\|"));
        }
        
        if (options == null || options.isEmpty()) {
            return null;
        }
        
        // Parse emotes
        Pattern emotesPattern = Pattern.compile("define\\s+emotes:\\s*<list\\[([^\\]]+)\\]>");
        Matcher emotesMatcher = emotesPattern.matcher(nestedChooseContent);
        List<String> emoteList = null;
        if (emotesMatcher.find()) {
            String listContent = emotesMatcher.group(1);
            emoteList = Arrays.asList(listContent.split("\\|"));
        }
        
        // Parse nested case blocks
        Map<String, String> nestedOptionTexts = new LinkedHashMap<>();
        Map<String, String> nestedOptionEmotes = new LinkedHashMap<>();
        Map<String, List<String>> nestedOptionExpressions = new LinkedHashMap<>();
        Map<String, List<String>> nestedNarrateMessages = new LinkedHashMap<>();
        Map<String, Integer> nestedNarrateDurations = new LinkedHashMap<>();
        Map<String, Integer> nestedNextSteps = new LinkedHashMap<>();
        Map<String, DialogOptions> nestedNestedDialogs = new LinkedHashMap<>();
        Map<String, ScreenFadeData> nestedScreenFades = new LinkedHashMap<>();
        Map<String, EyesCloseData> nestedEyesCloses = new LinkedHashMap<>();
        Map<String, ItemEquipData> nestedItemEquips = new LinkedHashMap<>();
        Map<String, AudioData> nestedAudioData = new LinkedHashMap<>(); // For backward compatibility
        Map<String, List<AudioData>> nestedAudioDataList = new LinkedHashMap<>(); // For multiple audio support
        
        // Expected indentation for nested cases (one level more than nested choose)
        int expectedNestedCaseIndent = chooseIndent + 4;
        
        Pattern nestedCasePattern = Pattern.compile("^(\\s+)-\\s+case\\s+([^:]+?)\\s*:", Pattern.MULTILINE);
        Matcher nestedCaseMatcher = nestedCasePattern.matcher(nestedChooseContent);
        
        List<CaseMatch> nestedCaseMatches = new ArrayList<>();
        while (nestedCaseMatcher.find()) {
            String indentStr = nestedCaseMatcher.group(1);
            int caseIndent = indentStr.length();
            String caseId = nestedCaseMatcher.group(2).trim();
            
            // Only include cases at the expected indentation level
            if (Math.abs(caseIndent - expectedNestedCaseIndent) <= 2) {
                nestedOptionTexts.put(caseId, formatOptionText(caseId));
                nestedCaseMatches.add(new CaseMatch(caseId, nestedCaseMatcher.start(), nestedCaseMatcher.end()));
            }
        }
        
        // Parse content for each nested case
        for (int i = 0; i < nestedCaseMatches.size(); i++) {
            CaseMatch currentCase = nestedCaseMatches.get(i);
            int startPos = currentCase.end();
            int endPos = (i < nestedCaseMatches.size() - 1) ? nestedCaseMatches.get(i + 1).start() : nestedChooseContent.length();
            
            String nestedCaseContent = nestedChooseContent.substring(startPos, endPos);
            List<String> messages = parseNarrateMessages(nestedCaseContent);
            List<String> expressions = parseExpressionsList(nestedCaseContent);
            int duration = parseNarrateDuration(nestedCaseContent);
            int nextStep = parseNextStep(nestedCaseContent);
            ScreenFadeData screenFade = parseScreenFade(nestedCaseContent);
            EyesCloseData eyesClose = parseEyesClose(nestedCaseContent);
            ItemEquipData itemEquip = parseItemEquip(nestedCaseContent);
            List<AudioData> nestedAudioList = parseAudioCommands(nestedCaseContent);
            AudioData audio = nestedAudioList.isEmpty() ? null : nestedAudioList.get(0); // For backward compatibility
            
            // Recursively parse nested choose blocks (for deeper nesting)
            DialogOptions deeperNested = parseNestedChooseBlock(nestedCaseContent, defaultDuration);
            if (deeperNested != null && !deeperNested.texts().isEmpty()) {
                nestedNestedDialogs.put(currentCase.caseId(), deeperNested);
            }
            
            if (!messages.isEmpty()) {
                nestedNarrateMessages.put(currentCase.caseId(), messages);
                nestedNarrateDurations.put(currentCase.caseId(), duration > 0 ? duration : defaultDuration);
            }
            if (expressions != null && !expressions.isEmpty()) {
                nestedOptionExpressions.put(currentCase.caseId(), expressions);
            }
            if (nextStep > 0) {
                nestedNextSteps.put(currentCase.caseId(), nextStep);
            }
            if (screenFade != null) {
                nestedScreenFades.put(currentCase.caseId(), screenFade);
            }
            if (eyesClose != null) {
                nestedEyesCloses.put(currentCase.caseId(), eyesClose);
            }
            if (itemEquip != null) {
                nestedItemEquips.put(currentCase.caseId(), itemEquip);
            }
            if (!nestedAudioList.isEmpty()) {
                nestedAudioData.put(currentCase.caseId(), audio);
                nestedAudioDataList.put(currentCase.caseId(), nestedAudioList);
            }
        }
        
        // Add any options that don't have cases
        for (String option : options) {
            String trimmed = option.trim();
            if (!nestedOptionTexts.containsKey(trimmed)) {
                nestedOptionTexts.put(trimmed, formatOptionText(trimmed));
            }
        }
        
        // Attach emote ids by order if provided
        if (emoteList != null && !emoteList.isEmpty()) {
            for (int i = 0; i < options.size() && i < emoteList.size(); i++) {
                String optId = options.get(i).trim();
                String emoteId = emoteList.get(i).trim();
                if (!emoteId.isEmpty()) {
                    nestedOptionEmotes.put(optId, emoteId);
                }
            }
        }
        
        return new DialogOptions(nestedOptionTexts, nestedOptionEmotes, nestedOptionExpressions, nestedNarrateMessages, 
                nestedNarrateDurations, defaultDuration, nestedNextSteps, nestedNestedDialogs, nestedScreenFades, nestedEyesCloses, nestedItemEquips, nestedAudioData, nestedAudioDataList);
    }
    
    /**
     * Parses next step number from case content.
     * Looks for lines like: - step 2
     * Returns step number, or 0 if not found.
     */
    private static int parseNextStep(String caseContent) {
        Pattern stepPattern = Pattern.compile("-\\s+step\\s+(\\d+)");
        Matcher stepMatcher = stepPattern.matcher(caseContent);
        if (stepMatcher.find()) {
            int step = Integer.parseInt(stepMatcher.group(1));
            Marallyzen.LOGGER.debug("DialogScriptLoader: Found next step {} in case content", step);
            return step;
        }
        Marallyzen.LOGGER.debug("DialogScriptLoader: No next step found in case content: {}", caseContent.substring(0, Math.min(100, caseContent.length())));
        return 0; // No next step specified
    }
    
    /**
     * Formats option ID into readable text.
     */
    private static String formatOptionText(String optionId) {
        if (optionId == null || optionId.isEmpty()) {
            return "Option";
        }
        
        // Handle special cases with Russian translations
        switch (optionId) {
            case "bye":
                return "Пока";
            case "goal":
                return "Какова наша цель?";
            case "denofine":
                return "Что с Денофайном?";
            case "dragon_egg":
                return "Чем обладает яйцо дракона?";
            case "op":
                return "Кто такие «ОП»?";
            case "quest":
                return "Есть задание?";
            case "location":
                return "Где мы находимся?";
            case "help":
                return "Нужна помощь";
            default:
                // Convert snake_case or camelCase to readable text
                String text = optionId.replace("_", " ");
                text = text.substring(0, 1).toUpperCase() + text.substring(1);
                return text;
        }
    }
    
    /**
     * Reads file content as UTF-8 string.
     */
    private static String readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Scanner scanner = new Scanner(fis, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
    
    /**
     * Clears the dialog cache. Call this when scripts are reloaded.
     */
    public static void clearCache() {
        dialogCache.clear();
    }
}
