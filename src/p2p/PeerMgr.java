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

    public void clearPeers() {
        peerList.clear();
        System.out.println("Cleared peer list.");
    }

    public void addPeer(Peer peer) {
        if (!peerList.containsKey(peer.getPeerID())) {
            peerList.put(peer.getPeerID(), peer);
            System.out.println("Added peer: " + peer.getPeerID());
        }
    }

    public void discoverPeers(String selfIP, int selfPort) {
        try (DatagramSocket socket = new DatagramSocket(selfPort, InetAddress.getByName("0.0.0.0"))) {
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            byte[] buffer = "DISCOVER_PEER".getBytes();

            // Broadcast to subnet
            InetAddress broadcastAddress = InetAddress.getByName("10.22.249.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, 4113);
            socket.send(packet);
            System.out.println("Discovery packet sent.");

            // Receive responses
            socket.setSoTimeout(5000);
            while (true) {
                try {
                    DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(response);

                    String peerIP = response.getAddress().getHostAddress();
                    if (!peerIP.equals(selfIP) && !peerList.containsKey(peerIP)) {
                        System.out.println("Discovered peer: " + peerIP);
                        addPeer(new Peer(peerIP, peerIP, 4113));
                    }
                }
                catch (IOException e) {
                    System.out.println("Discovery timed out: " + e.getMessage());
                    break;
                }
            }
        }
        catch (IOException e) {
            System.err.println("Failed to discover peers: " + e.getMessage());
        }
    }

    public Peer getPeer(String peerID) { return peerList.get(peerID); }
    public Collection<Peer> getAllPeers() { return peerList.values(); }
}