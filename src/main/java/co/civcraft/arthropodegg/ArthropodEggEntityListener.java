package co.civcraft.arthropodegg;

import static co.civcraft.arthropodegg.ArthropodEggPlugin.log;

import java.util.Map;

import net.minecraft.server.v1_10_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;

/**
 * @author Randy
 * @since 1.0
 * <p>
 * All the event listeners related to ArthropodEggPlugin plugin. Handles
 * each case that is currently configured for handling.
 */
public class ArthropodEggEntityListener implements Listener {

	private ArthropodEggPlugin plugin;

	public ArthropodEggEntityListener(ArthropodEggPlugin instance) {
		plugin = instance;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		Player targetPlayer = event.getEntity().getKiller();
		if (null == targetPlayer) {
			return;
		}

		Short currentEntityID = event.getEntity().getType().getTypeId();
		if (!plugin.getConfig().getShortList("eggEntityIDList").contains(currentEntityID)) {
			return;
		}

		// Check for a baby animal
		if (event.getEntity() instanceof Ageable) {
			Ageable ageableEntity = (Ageable) event.getEntity();
			if (!ageableEntity.isAdult()) {
				return;  // NOPE.
			}
		}

		// Check the player's currently equipped weapon
		ItemStack handstack = targetPlayer.getItemInHand();
		// Get the map of enchantments on that item
		Map<Enchantment, Integer> itemEnchants = handstack.getEnchantments();
		if (itemEnchants.isEmpty()) {
			return;
		}

		// Check if one enchantment is BaneOfArthropods
		if (null == itemEnchants.get(org.bukkit.enchantments.Enchantment.DAMAGE_ARTHROPODS)) {
			return;
		}

		double randomNum = Math.random();
		double eggArthropodPercentage = plugin.getConfig().getDouble("eggArthropodPercentage");
		double eggLootingPercentage = plugin.getConfig().getDouble("eggLootingPercentage");
		double levelOfArthropod = handstack.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DAMAGE_ARTHROPODS);
		double levelOfLooting = handstack.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LOOT_BONUS_MOBS);

		double targetPercentage = (eggArthropodPercentage * levelOfArthropod) + (eggLootingPercentage *
				levelOfLooting);
		if (plugin.getConfig().getBoolean("eggDebug")) {
			targetPlayer.sendMessage("Arth[" + levelOfArthropod + "], Loot[" + levelOfLooting + "]");
			targetPlayer.sendMessage("Total =" + targetPercentage * 100 + "%, random% is " + randomNum * 100);
		}

		// Check if egg should be spawned
		if (randomNum < targetPercentage) {
			// Figure out the right item type to drop.
			SpawnEgg egg = new SpawnEgg(event.getEntityType());
			ItemStack item = egg.toItemStack(1);
			net.minecraft.server.v1_10_R1.ItemStack s = CraftItemStack.asNMSCopy(item);
			if (s == null) {
				log().severe("Attempted to create map conform copy of " + item.toString()
						+ ", but couldn't because this item can't be held in inventories since Minecraft 1.8");
				return;
			}
			NBTTagCompound nbt = s.getTag();
			if (nbt == null) {
				nbt = new NBTTagCompound();
			}
			NBTTagCompound eID = new NBTTagCompound();
			eID.setString("id", egg.getSpawnedType().getName());
			nbt.set("EntityTag", eID);
			s.setTag(nbt);
			item = CraftItemStack.asBukkitCopy(s);

			if (plugin.getConfig().getBoolean("eggRemoveDrops")) {
				event.getDrops().clear();
				event.setDroppedExp(0);
			}
			event.getDrops().add(item);
			if (plugin.getConfig().getBoolean("eggDebug")) {
				targetPlayer.sendMessage("Egg generated.");
			}
		}
	}
}
