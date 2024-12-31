package p2p;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PeerMgr {
    private final Map<String, Peer> peerList;

    public PeerMgr() {
        this.peerList = new HashMap<>();
    }

    public void addPeer(Peer peer) {
        peerList.put(peer.getPeerID(), peer);
    }

    public void discoverPeers() {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buffer = "DISCOVER_PEER".getBytes();
            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length,
                    InetAddress.getByName("255.255.255.255"),
                    4113
            );
            socket.send(packet);
            System.out.println("Discovery packet sent on port 4113.");
        } catch (IOException e) {
            System.out.println("Failed to discover peers: " + e.getMessage());
        }
    }

    public Peer getPeer(String peerID) { return peerList.get(peerID); }
    public Collection<Peer> getAllPeers() { return peerList.values(); }
}