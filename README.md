# P2P File Transfer Application
---

## Prerequisites
* Java 17
* Docker & Docker Compose

## Setting up Macvlan Network for Docker and Host Communication

### 1. Create the Macvlan Network

Run the following command to create a macvlan network in bridge mode:

```bash
docker network create \
    -d macvlan \
    --subnet=<subnet> \
    --gateway=<gateway> \
    -o parent=<network interface> \
    -o macvlan_mode=bridge \
    macvlan_network

### Example Network Details (mine lol)
- **Subnet**: `10.22.0.0/16`
- **Gateway**: `10.22.249.1`
- **Container IP Range**: `10.22.249.201` to `10.22.249.203`
- **Host Macvlan IP**: `10.22.249.250`
```
### 2. Setup the Virtual Interface
Create the virtual interface:
```bash
sudo ip link add macvlan0 link wlp0s20f3 type macvlan mode bridge
````
Assign a unique IP and start it:
```bash
sudo ip addr add 10.22.249.250/16 dev macvlan0
udo ip link set macvlan0 up
```