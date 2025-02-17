# P2P File Transfer Application

A term project for the CSE471 course, allows for P2P file sharing between peers in a network.
The network is populated by containers running Alpine Linux.
To bridge communication between the Docker network and the local machine, a MacVLAN network is utilized.


## Prerequisites
* Java 17
* Docker & Docker Compose

## Steps to Run

### 1. Clear existing Docker networks
```bash
sudo docker network prune
```

### 2. Create the MacVLAN Interface
```bash
sudo ip link add macvlan_bridge link wlp0s20f3 type macvlan mode bridge
sudo ip addr add 10.22.249.100/24 dev macvlan_bridge
sudo ip link set macvlan_bridge up
```
Substitute **wlp0s20f3** with your networking interface, which can be found via command:
`ip link show`

**10.22.249.199/24** is a local IP address, ensure that it doesn't conflict with other devices within the network.

### 3. Bring up the Docker containers
```bash
docker-compose up --build
```
This initializes three peers running on Alpine Linux, as well as the MacVLAN network.

If necessary, adjust the number of peers, as well as your gateway and subnet addresses (find them via `ip route`), in the `docker-compose.yml` file!

## Additional Notes
1. MacVLAN does not persist across sessions, so make sure to repeat its initialization.
2. If network bridging does not work, consider:
   1. disabling ufw via `sudo ufw disable`,
   2. freeing up the ports 4113 and 4114 (my lucky numbers!),
   3. and pinging the containers from the local machine and vice versa:
      * From local machine: `ping 10.22.249.201` to `ping 10.22.249.203`
      * From container: `docker exec -it peer1 ping 10.22.249.100`