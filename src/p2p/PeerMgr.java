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
        System.out.println("[PeerMgr] Cleared peer list.");
    }

    public void addPeer(Peer peer) {
        if (!peerList.containsKey(peer.getPeerID())) {
            peerList.put(peer.getPeerID(), peer);
            System.out.println("[PeerMgr] Added peer => " + peer.getPeerID());
        }
    }

    public void discoverPeers(String selfIP, int selfPort) {
        try (DatagramSocket socket = new DatagramSocket(selfPort, InetAddress.getByName("0.0.0.0"))) {
            socket.setReuseAddress(true);
            socket.setBroadcast(true);

            byte[] buffer = "DISCOVER_PEER".getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("10.22.249.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, 4113);
            socket.send(packet);
            System.out.println("[PeerMgr] UDP flood => broadcasted 'DISCOVER_PEER' to " + broadcastAddress);

            socket.setSoTimeout(5000);
            while (true) {
                try {
                    DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(response);

                    String discoveredIP = response.getAddress().getHostAddress();
                    if (!discoveredIP.equals(selfIP) && !peerList.containsKey(discoveredIP)) {
                        System.out.println("[PeerMgr] Discovered => " + discoveredIP);
                        Peer newPeer = new Peer(discoveredIP, discoveredIP, 4113);
                        addPeer(newPeer);

                        // get that peer's file list
                        var sharedFiles = FileClient.requestSharedFiles(discoveredIP, 4113);
                        System.out.println("[PeerMgr] " + discoveredIP + " returned "
                                + sharedFiles.size() + " files.");

                        // store them as "hash_filename"
                        for (SimpleFileInfo info : sharedFiles) {
                            File pseudoFile = new File(info.fileHash + "_" + info.fileName);
                            newPeer.addSharedFile(pseudoFile);
                        }
                    }
                }
                catch (SocketTimeoutException e) {
                    System.out.println("[PeerMgr] UDP flood => discovery timed out => " + e.getMessage());
                    break;
                }
            }
        }
        catch (IOException e) {
            System.err.println("[PeerMgr] discoverPeers => " + e.getMessage());
        }
    }

    public Peer getPeer(String peerID)      { return peerList.get(peerID); }

    public Collection<Peer> getAllPeers()   { return peerList.values(); }
}