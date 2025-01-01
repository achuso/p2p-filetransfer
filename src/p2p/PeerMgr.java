package p2p;

import network.FileClient;
import network.FileClient.SimpleFileInfo;

import java.io.File;
import java.io.IOException;
import java.net.*;
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

                        // Create a new Peer object
                        Peer newPeer = new Peer(peerIP, peerIP, 4113);
                        addPeer(newPeer);

                        // Request that peer’s shared files
                        var sharedFiles = FileClient.requestSharedFiles(peerIP, 4113);
                        for (SimpleFileInfo info : sharedFiles) {
                            // Construct a File to store in newPeer
                            File pseudoFile = new File(info.fileName);

                            // Add to the peer’s known shared files
                            newPeer.addSharedFile(pseudoFile);
                        }
                    }
                }
                catch (IOException e) {
                    System.err.println("Discovery timed out: " + e.getMessage());
                    break;
                }
            }
        }
        catch (IOException e) {
            System.err.println("Failed to discover peers: " + e.getMessage());
        }
    }

    public Peer getPeer(String peerID) {
        return peerList.get(peerID);
    }

    public Collection<Peer> getAllPeers() {
        return peerList.values();
    }
}