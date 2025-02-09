package com.eatsleepcode.tunnelbuilder;

import cn.nukkit.block.BlockChest;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.block.Block;
import cn.nukkit.Player;

public class TunnelBuilder extends PluginBase {

	@Override
	public void onEnable() {
		getLogger().info(TextFormat.GREEN + "TunnelBuilder Loaded!");
	}

	@Override
	public void onDisable() {
		getLogger().info(TextFormat.RED + "TunnelBuilder Disabled!");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			if (command.getName().equalsIgnoreCase("tunnel")) {
				if (!(sender instanceof Player)) {
					sender.sendMessage(TextFormat.RED + "This command can only be run by a player.");
					return true;
				}
	
				Player player = (Player) sender;
	
				int length;
				String tunnelType = "default";
				int startX = (int) player.getX();
				int startY = (int) player.getY() - 1;
				int startZ = (int) player.getZ();
				String direction = getPlayerDirection(player);
	
				try {
					// Parse arguments
					if (args.length >= 1) {
						length = Integer.parseInt(args[0]);
					} else {
						sender.sendMessage(TextFormat.RED + "Usage: /tunnel <length> [type] [direction] [x] [y] [z]");
						return false;
					}
					if (args.length >= 2) {
						tunnelType = args[1].toLowerCase();
					}
					if (args.length >= 3) {
						direction = args[2].toLowerCase();
					}
					if (args.length >= 6) {
						startX = Integer.parseInt(args[3]);
						startY = Integer.parseInt(args[4]);
						startZ = Integer.parseInt(args[5]);
					}
					
	
					Level level = player.getLevel();
	
					// Wrap buildTunnel with its own try-catch for extra safety
					try {
						buildTunnel(level, length, tunnelType, direction, startX, startY, startZ, sender);
					} catch (Exception e) {
						sender.sendMessage(TextFormat.RED + "An error occurred while building the tunnel.");
						getLogger().error("Error during tunnel construction", e);
					}
	
				} catch (NumberFormatException e) {
					sender.sendMessage(TextFormat.RED + "Error: Invalid number format in arguments.");
				} catch (IllegalArgumentException e) {
					sender.sendMessage(TextFormat.RED + e.getMessage());
				}
	
				return true;
			}
	
		} catch (Exception e) {
			sender.sendMessage(TextFormat.RED + "An unexpected error occurred.");
			getLogger().error("Unexpected error in onCommand", e);
		}
	
		return false;
	}
	

	private void buildTunnel(Level level, int length, String tunnelType, String direction, int startX, int startY, int startZ, CommandSender sender) {
		int dx = 0, dz = 0;
		boolean isNorthSouth = false;

		switch (direction) {
			case "north":
			case "n":
				dz = -1;
				isNorthSouth = true;
				break;
			case "south":
			case "s":
				dz = 1;
				isNorthSouth = true;
				break;
			case "west":
			case "w":
				dx = -1;
				isNorthSouth = false;
				break;
			case "east":
			case "e":
				dx = 1;
				isNorthSouth = false;
				break;
			default:
				throw new IllegalArgumentException("Invalid direction. Use north, south, west, or east.");
		}

		switch (tunnelType) {
			case "rail":
				// === RAIL TUNNEL ============================================
				for (int i = 0; i < length; i++) {
					int x = startX + i * dx;
					int z = startZ + i * dz;
					level.loadChunk(x >> 4, z >> 4, true);

					// Iterate through the height of the tunnel
					for (int y = startY + 5; y >= startY; y--) {
						for (int offset = -4; offset <= 4; offset++) {
							int wallX = isNorthSouth ? x + offset : x;
							int wallZ = isNorthSouth ? z : z + offset;

							// Call flood prevention for the current position
							preventFlooding(level, wallX, y, wallZ);

							if (y == startY || y == startY + 4 || y == startY + 5) {
								// Floor and ceiling
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.STONE));
							} else if (offset == -4 || offset == 4) {
								// Walls
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.STONE));
							} else {
								// Air space
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.AIR));
							}

							// Place sea lanterns in the ceiling every 5 blocks
							if (y == startY + 4 && (offset == -2 || offset == 2) && i % 5 == 0) {
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.SEA_LANTERN));
							}

							// Place redstone block in the floor every 5 blocks
							if (y == startY && (offset == -2 || offset == 2) && i % 5 == 0) {
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.REDSTONE_BLOCK));
							}
						}
					}
				}

				// Rails (must be placed after tunnel is built)
				for (int i = 0; i < length; i++) {
					int x = startX + i * dx;
					int z = startZ + i * dz;
					if (!level.isChunkLoaded(x >> 4, z >> 4)) {
						level.loadChunk(x >> 4, z >> 4, true);
					}

					for (int y = startY + 5; y >= startY; y--) {
						for (int offset = -4; offset <= 4; offset++) {
							int wallX = isNorthSouth ? x + offset : x;
							int wallZ = isNorthSouth ? z : z + offset;

							// Place powered rail on top of redstone blocks
							if (y == startY + 1 && (offset == -2 || offset == 2)) {
								Block poweredRail = Block.get(Block.POWERED_RAIL);
								// Set rail orientation based on the tunnel direction
								poweredRail.setDamage(isNorthSouth ? 0 : 1); // 0 for North-South, 1 for East-West
								level.setBlock(new Vector3(wallX, y, wallZ), poweredRail);
							}
						}
					}
				}

				sender.sendMessage(TextFormat.GREEN + "Rail tunnel built successfully!");
				// ============================================================

				break;

			case "farm":
				// === FARM TUNNEL ============================================
				for (int i = 0; i < length; i++) {
					int x = startX + i * dx;
					int z = startZ + i * dz;
					if (!level.isChunkLoaded(x >> 4, z >> 4)) {
						level.loadChunk(x >> 4, z >> 4, true);
					}

					// Iterate through the height of the tunnel
					for (int y = startY + 5; y >= startY; y--) {
						for (int offset = -4; offset <= 4; offset++) {
							int wallX = isNorthSouth ? x + offset : x;
							int wallZ = isNorthSouth ? z : z + offset;

							// Call flood prevention for the current position
							preventFlooding(level, wallX, y, wallZ);

							if (y == startY || y == startY + 4 || y == startY + 5) {
								// Floor and ceiling
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.STONE));
							} else if (offset == -4 || offset == 4) {
								// Walls
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.STONE));
							} else {
								// Air space
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.AIR));
							}

							// Place sea lanterns in the ceiling every 5 blocks
							if (y == startY + 4 && (offset == -2 || offset == 2) && i % 5 == 0) {
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.SEA_LANTERN));
							}

							// Dirt Path
							if (y == startY && offset == 0) {
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(198)); // FOR SOME REASON API IS MISSING DIRT_PATH ENTITY NAME SO NEED TO USE ID INSTEAD
							}

							// Farmland
							if (y == startY && (offset == -1 || offset == 1 || offset == -3 || offset == 3)) {
								Vector3 farmlandPosition = new Vector3(wallX, y, wallZ);
								
								// Create a farmland block and set its moisture level to fully hydrated (7)
								Block farmland = Block.get(Block.FARMLAND);
								farmland.setDamage(7); 
								
								// Place the block in the level with updates for lighting and physics
								level.setBlock(farmlandPosition, farmland, true, true);
							}

							// Water
							if (y == startY && (offset == -2 || offset == 2)) {
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.WATER));
							}
						}
					}
				}

				// Crops (must be planted after farmland exists)
				for (int i = 0; i < length; i++) {
					int x = startX + i * dx;
					int z = startZ + i * dz;
					if (!level.isChunkLoaded(x >> 4, z >> 4)) {
						level.loadChunk(x >> 4, z >> 4, true);
					}

					for (int y = startY + 5; y >= startY; y--) {
						for (int offset = -4; offset <= 4; offset++) {
							int wallX = isNorthSouth ? x + offset : x;
							int wallZ = isNorthSouth ? z : z + offset;

							// Chests
							if (y == startY + 1 && offset == -1 && i % 40 == 0) {
								createChest(level, new Vector3(wallX, y, wallZ), "melons");
							} else if (y == startY + 1 && offset == -1 && i % 30 == 0) {
								createChest(level, new Vector3(wallX, y, wallZ), "wheat");
							} else if (y == startY + 1 && offset == -1 && i % 20 == 0) {
								createChest(level, new Vector3(wallX, y, wallZ), "carrots");
							} else if (y == startY + 1 && offset == -1 && i % 10 == 0) {
								createChest(level, new Vector3(wallX, y, wallZ), "potato");
							} else if (y == startY + 1 && (offset == -1 || offset == 1 || offset == -3 || offset == 3)) {
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.POTATO_BLOCK));
							}
						}
					}
				}
				
				sender.sendMessage(TextFormat.GREEN + "Farm tunnel built successfully!");
				// ============================================================

				break;

			case "empty":
				// === EMPTY TUNNEL ===========================================
				for (int i = 0; i < length; i++) {
					int x = startX + i * dx;
					int z = startZ + i * dz;
					if (!level.isChunkLoaded(x >> 4, z >> 4)) {
						level.loadChunk(x >> 4, z >> 4, true);
					}

					// Iterate through the height of the tunnel
					for (int y = startY + 5; y >= startY; y--) {
						for (int offset = -4; offset <= 4; offset++) {
							int wallX = isNorthSouth ? x + offset : x;
							int wallZ = isNorthSouth ? z : z + offset;

							level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.AIR));
						}
					}
				}
				sender.sendMessage(TextFormat.YELLOW + "Tunnel emptied successfully!");
				// ============================================================

				break;

			default:

				// === DEFAULT TUNNEL =========================================
				for (int i = 0; i < length; i++) {
					int x = startX + i * dx;
					int z = startZ + i * dz;
					if (!level.isChunkLoaded(x >> 4, z >> 4)) {
						level.loadChunk(x >> 4, z >> 4, true);
					}

					// Iterate through the height of the tunnel
					for (int y = startY + 5; y >= startY; y--) {
						for (int offset = -2; offset <= 2; offset++) {
							int wallX = isNorthSouth ? x + offset : x;
							int wallZ = isNorthSouth ? z : z + offset;

							// Call flood prevention for the current position
							preventFlooding(level, wallX, y, wallZ);

							if (y == startY || y == startY + 4 || y == startY + 5) {
								// Floor and ceiling
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.STONE));
							} else if (offset == -2 || offset == 2) {
								// Walls
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.STONE));
							} else {
								// Air space
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.AIR));
							}

							// Place sea lanterns in the ceiling every 5 blocks
							if (y == startY + 4 && offset == 0 && i % 5 == 0) {
								level.setBlock(new Vector3(wallX, y, wallZ), Block.get(Block.SEA_LANTERN));
							}
						}
					}
				}
				sender.sendMessage(TextFormat.GREEN + "Tunnel built successfully!");
				// ============================================================

		}
	}

	private String getPlayerDirection(Player player) {
		float yaw = (float) player.getYaw(); // Cast to float for Nukkit
		if (yaw < 0) {
			yaw += 360;
		}
		yaw %= 360;

		if (yaw >= 315 || yaw < 45) {
			return "south";
		} else if (yaw >= 45 && yaw < 135) {
			return "west";
		} else if (yaw >= 135 && yaw < 225) {
			return "north";
		} else {
			return "east";
		}
	}

	private void preventFlooding(Level level, int x, int y, int z) {
		Block block = level.getBlock(new Vector3(x, y, z));
		if (block.getId() == Block.WATER || block.getId() == Block.LAVA || block.getId() == Block.GRAVEL || block.getId() == Block.SAND || block.getId() == Block.CONCRETE_POWDER) {
			level.setBlock(new Vector3(x, y, z), Block.get(Block.AIR));
		}
	}
	

	public void createChest(Level level, Vector3 position, String contents) {
		// Create and place the chest block
		BlockChest chestBlock = new BlockChest();
		level.setBlock(position, chestBlock);

		// Create the NBT compound for the BlockEntity
		CompoundTag nbt = new CompoundTag()
				.putString("id", "Chest")
				.putInt("x", position.getFloorX())
				.putInt("y", position.getFloorY())
				.putInt("z", position.getFloorZ());

		// Create the BlockEntityChest for the chest
		BlockEntity blockEntity = BlockEntity.createBlockEntity("Chest", level.getChunk(position.getFloorX() >> 4, position.getFloorZ() >> 4), nbt);
		if (blockEntity instanceof BlockEntityChest) {
			BlockEntityChest chestEntity = (BlockEntityChest) blockEntity;

			// Add the BlockEntity to the world
			chestEntity.spawnToAll();

			Item hoeItem = Item.get(Item.WOODEN_HOE, 0, 1); // A wooden hoe

			// Create the seed item
			Item chestItems;
			switch (contents.toLowerCase()) {
				case "potatoes":
					chestItems = Item.get(Item.POTATO, 0, 64);
					break;
				case "carrots":
					chestItems = Item.get(Item.CARROT, 0, 64);
					break;
				case "wheat":
					chestItems = Item.get(Item.WHEAT_SEEDS, 0, 64);
					break;
				case "melons":
					chestItems = Item.get(Item.MELON_SEEDS, 0, 64);
					break;
				default:  
					chestItems = Item.get(Item.SEEDS, 0, 64); 
				}

			// Put a hoe in the chest
			chestEntity.getInventory().setItem(0, hoeItem);

			// Fill the remaining chest inventory with "seeds"
			for (int i = 1; i < chestEntity.getSize(); i++) {
				chestEntity.getInventory().setItem(i, chestItems.clone());
			}

		} else {
			System.out.println("Failed to create BlockEntityChest at the specified position.");
		}
	}
}
