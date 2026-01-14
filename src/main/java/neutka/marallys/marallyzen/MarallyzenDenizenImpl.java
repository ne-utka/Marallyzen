package neutka.marallys.marallyzen;

import com.denizenscript.denizencore.DenizenImplementation;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import neutka.marallys.marallyzen.objects.LocationTag;
import com.denizenscript.denizencore.objects.core.VectorObject;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.DefinitionProvider;

import java.io.File;

public class MarallyzenDenizenImpl implements DenizenImplementation {

    @Override
    public File getScriptFolder() {
        return DenizenService.getScriptsFolder();
    }

    @Override
    public String getImplementationVersion() {
        return DenizenService.getImplementationVersion();
    }

    @Override
    public String getImplementationName() {
        return "NeoForge";
    }

    @Override
    public void preScriptReload() {
    }

    @Override
    public void onScriptReload() {
    }

    @Override
    public String queueHeaderInfo(ScriptEntry scriptEntry) {
        return "";
    }

    @Override
    public boolean needsHandleArgPrefix(String prefix) {
        return false;
    }

    @Override
    public boolean handleCustomArgs(ScriptEntry scriptEntry, Argument argument) {
        return false;
    }

    @Override
    public void refreshScriptContainers() {
    }

    @Override
    public TagContext getTagContext(ScriptContainer scriptContainer) {
        return new MarallyzenTagContext(scriptContainer);
    }

    @Override
    public TagContext getTagContext(ScriptEntry scriptEntry) {
        return new MarallyzenTagContext(scriptEntry);
    }

    @Override
    public ScriptEntryData getEmptyScriptEntryData() {
        return new MarallyzenScriptEntryData();
    }

    @Override
    public String cleanseLogString(String input) {
        return input;
    }

    @Override
    public VectorObject vectorize(ObjectTag objectTag, TagContext tagContext) {
        // Convert ObjectTag to VectorObject (LocationTag)
        return objectTag.asType(LocationTag.class, tagContext);
    }

    @Override
    public VectorObject getVector(double x, double y, double z) {
        return new LocationTag(x, y, z);
    }

    @Override
    public void preTagExecute() {
    }

    @Override
    public void postTagExecute() {
    }

    @Override
    public boolean canWriteToFile(File f) {
        return true; // Allow file writing by default
    }

    @Override
    public String getRandomColor() {
        return "§" + Integer.toHexString((int) (Math.random() * 16));
    }

    @Override
    public boolean canReadFile(File f) {
        return true; // Allow file reading by default
    }

    @Override
    public File getDataFolder() {
        return DenizenService.getScriptsFolder().getParentFile();
    }

    @Override
    public FlaggableObject simpleWordToFlaggable(String word, ScriptEntry entry) {
        return null; // Not implemented yet
    }

    @Override
    public ObjectTag getSpecialDef(String def, ScriptQueue queue) {
        return null; // Not implemented yet
    }

    @Override
    public boolean setSpecialDef(String def, ScriptQueue queue, ObjectTag value) {
        return false; // Not implemented yet
    }

    @Override
    public void addExtraErrorHeaders(StringBuilder headerBuilder, ScriptEntry source) {
    }

    @Override
    public String applyDebugColors(String uncolored) {
        return uncolored; // Basic implementation
    }

    @Override
    public void doFinalDebugOutput(String rawText) {
        Marallyzen.LOGGER.info(rawText);
    }

    @Override
    public void addFormatScriptDefinitions(DefinitionProvider provider, TagContext context) {
    }

    @Override
    public String stripColor(String message) {
        return message.replaceAll("§[0-9a-fk-or]", ""); // Basic color stripping
    }

    @Override
    public void reloadConfig() {
        // Reload config if needed
    }

    @Override
    public void reloadSaves() {
        // Reload saves if needed
    }
}


