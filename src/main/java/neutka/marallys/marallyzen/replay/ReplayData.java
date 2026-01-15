package neutka.marallys.marallyzen.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReplayData {
    private final String id;
    private final ReplayHeader header;
    private final List<ReplayMarker> markers;
    private final ReplayServerTrack serverTrack;
    private final ReplayClientTrack clientTrack;

    public ReplayData(String id, ReplayHeader header, List<ReplayMarker> markers,
                      ReplayServerTrack serverTrack, ReplayClientTrack clientTrack) {
        this.id = id;
        this.header = header;
        this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
        this.serverTrack = serverTrack;
        this.clientTrack = clientTrack;
    }

    public String getId() {
        return id;
    }

    public ReplayHeader getHeader() {
        return header;
    }

    public List<ReplayMarker> getMarkers() {
        return Collections.unmodifiableList(markers);
    }

    public ReplayServerTrack getServerTrack() {
        return serverTrack;
    }

    public ReplayClientTrack getClientTrack() {
        return clientTrack;
    }
}
