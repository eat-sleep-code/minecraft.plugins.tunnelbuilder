#TunnelBuilder

Build a lighted tunnel with configurable length and direction.  Choose from a standard stone tunnel, a parallel powered rail tunnel, or a farm tunnel.

## Prerequisites
- [Nukkit Minecraft Server](https://github.com/PetteriM1/NukkitPetteriM1Edition/releases)

## Installation 
- Place the `TunnelBuilder.jar` file in the `<Nukkit Installation Folder>/plugins/` folder.

## Usage

- Create a 3x3 lighted tunnel that is 20 blocks long:

  `/tunnel 20`

- Create a lighted farm tunnel (with two canals and four rows of crops) that is 10 blocks long:

  `/tunnel 10 farm`

- Create a lighted rail tunnel (with two rails) that is 100 blocks long:

  `/tunnel 100 rail`

- By default, the tunnel will be built starting from your character's position, in the direction the character is facing.   You may override this by specifying the direction (north, south, east, or west) and/or X and Y coordinates:

  `/tunnel <length> [type] [direction] [x] [y] [z]`
  

