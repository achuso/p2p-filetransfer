package p2p;

import network.FileClient;
import network.FileClient.FileInfo;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PeerMgr {
    private final Map<String, Peer> peerList;

    private static final int BROADCAST_LIMIT = 20;
    private int broadcastCount = 0;

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
        if (broadcastCount >= BROADCAST_LIMIT) {
            System.out.println("Broadcast limit reached-- no more flooding.");
            return;
        }
        broadcastCount++;

        try (DatagramSocket socket = new DatagramSocket(selfPort, InetAddress.getByName("0.0.0.0"))) {
            socket.setReuseAddress(true);
            socket.setBroadcast(true);

            byte[] buffer = "DISCOVER_PEER".getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("10.22.249.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, 4113);
            socket.send(packet);

            socket.setSoTimeout(5000);
            while (true) {
                try {
                    DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(response);

                    String discoveredIP = response.getAddress().getHostAddress();
                    if (!discoveredIP.equals(selfIP) && !peerList.containsKey(discoveredIP)) {
                        System.out.println("Discovered IP:" + discoveredIP);
                        Peer newPeer = new Peer(discoveredIP, discoveredIP, 4113);
                        addPeer(newPeer);

                        var shared = FileClient.requestSharedFiles(discoveredIP, 4113);

                        for (FileInfo info : shared) {
                            File pseudoFile = new File(info.fileHash + "_" + info.fileName);
                            newPeer.addSharedFile(pseudoFile);
                        }
                    }
                }
                catch (SocketTimeoutException e) {
                    System.out.println(e.getMessage());
                    break;
                }
            }
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public Collection<Peer> getAllPeers() { return peerList.values(); }
}