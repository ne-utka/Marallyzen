package neutka.marallys.marallyzen.replay;

import java.util.ArrayList;
import java.util.List;

public final class ReplayMerger {
    private ReplayMerger() {
    }

    public static ReplayData merge(ReplayData serverData, ReplayData clientData, String replayId) {
        if (serverData == null && clientData == null) {
            return null;
        }
        ReplayHeader header = serverData != null ? serverData.getHeader() : clientData.getHeader();
        ReplayServerTrack serverTrack = serverData != null ? serverData.getServerTrack() : null;
        ReplayClientTrack clientTrack = clientData != null ? clientData.getClientTrack() : null;

        List<ReplayMarker> markers = new ArrayList<>();
        if (clientData != null && clientData.getMarkers() != null) {
            markers.addAll(clientData.getMarkers());
        } else if (serverData != null && serverData.getMarkers() != null) {
            markers.addAll(serverData.getMarkers());
        }

        return new ReplayData(replayId, header, markers, serverTrack, clientTrack);
    }
}
