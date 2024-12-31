package p2p;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class PeerManager {
    private Map<String, Peer> peerList;

    public PeerManager() {
        this.peerList = new HashMap<>();
    }

    public void addPeer(Peer peer) {
        peerList.put(peer.getPeerID(), peer);
    }

    public Peer getPeer(String peerID) {
        return peerList.get(peerID);
    }

    public Collection<Peer> getAllPeers() {
        return peerList.values();
    }

    public void discoverPeers() {
    }

//    public void discoverPeers() {
//    }
}