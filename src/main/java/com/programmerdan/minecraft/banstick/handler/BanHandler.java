package com.programmerdan.minecraft.banstick.handler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.programmerdan.minecraft.banstick.BanStick;
import com.programmerdan.minecraft.banstick.containers.BanResult;
import com.programmerdan.minecraft.banstick.data.BSBan;
import com.programmerdan.minecraft.banstick.data.BSIP;
import com.programmerdan.minecraft.banstick.data.BSPlayer;
import com.programmerdan.minecraft.banstick.data.BSSession;

/**
 * A series of static utility classes to facilitate issuing bans.
 * 
 * @author <a href="mailto:programmerdan@gmail.com">ProgrammerDan</a>
 *
 */
public class BanHandler {
	private static SimpleDateFormat endTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mms:ss");
	private BanHandler() {}

	/**
	 * Issues a ban against a specific UUID.
	 * 
	 * If the player is online, kicks them.
	 * 
	 * This uses the default message. See {@link #doUUIDBan(UUID, String, boolean)} for custom message,
	 * or {@link #doUUIDBan(UUID, Date, boolean)} for end time, or {@link #doUUIDBan(UUID, String, Date, boolean)} for
	 * both.
	 * 
	 * @param playerId
	 *  The UUID of the player to ban.
	 * @param adminBan
	 *  Was this admin ban or automatic?
	 * @return
	 *  A summary of who was banned.
	 */
	public static BanResult doUUIDBan(UUID playerId, boolean adminBan) {
		return doUUIDBan(playerId, null, null, adminBan);
	}
	
	/**
	 * Issues a ban against a specific UUID.
	 * 
	 * If the player is online, kicks them.
	 * 
	 * This uses the default message but a custom end time.
	 * 
	 * @param playerId
	 *  The UUID of the player to ban.
	 * @param banEnd
	 *  The time the ban should end.
	 * @param adminBan
	 *  Was this admin ban or automatic?
	 * @return
	 *  A summary of who was banned.
	 */
	public static BanResult doUUIDBan(UUID playerId, Date banEnd, boolean adminBan) {
		return doUUIDBan(playerId, null, banEnd, adminBan);
	}
	
	/**
	 * Issues a ban against a specific UUID.
	 * 
	 * If the player is online, kicks them.
	 * 
	 * This uses a custom message.
	 * 
	 * @param playerId
	 *  The UUID of the player to ban.
	 * @param message
	 *  The message to display when a player attempts to rejoin.
	 * @param adminBan
	 *  Was this admin ban or automatic?
	 * @return
	 *  A summary of who was banned.
	 */
	public static BanResult doUUIDBan(UUID playerId, String message, boolean adminBan) {
		return doUUIDBan(playerId, message, null, adminBan);
	}
	
	/**
	 * Issues a ban against a specific UUID.
	 * 
	 * If the player is online, kicks them.
	 * 
	 * This uses a custom message and end time.
	 * 
	 * @param playerId
	 *  The UUID of the player to ban.
	 * @param message
	 *  The message to display when a player attempts to rejoin.
	 * @param banEnd
	 *  The time the ban should end.
	 * @param adminBan
	 *  Was this admin ban or automatic?
	 * @return
	 *  A summary of who was banned.
	 */
	public static BanResult doUUIDBan(UUID playerId, String message, Date banEnd, boolean adminBan) {
		try {
			if (message == null || message.trim().equals("")) {
				message = adminBan ? "Administrative Ban" : "Automatic Ban"; // TODO: config!
			}
			Player spigotPlayer = Bukkit.getPlayer(playerId);
			BSPlayer player = BSPlayer.byUUID(playerId);
			if (player == null) {
				if (spigotPlayer != null) {
					player = BSPlayer.create(spigotPlayer);
				} else {
					player = BSPlayer.create(playerId);
				}
			}
			BSBan ban = BSBan.create(message, banEnd, adminBan); // general ban.
			player.setBan(ban);
			
			if (spigotPlayer != null) {
				if (banEnd != null) {
					spigotPlayer.kickPlayer(message + ". Ends " + BanHandler.endTimeFormat.format(banEnd));
				} else {
					spigotPlayer.kickPlayer(message);
				}
			}
			
			BanResult result = new BanResult();
			result.addPlayer(player);
			return result;
		} catch (Exception e) {
			BanStick.getPlugin().warning("Failed to issue UUID ban: ", e);
			return new BanResult();
		}
	}

	/**
	 * Issues a ban against an IP address.
	 * After the ban is created, finds all accounts that are using the IP address and bans them, unless
	 * already banned or pardoned.
	 * 
	 * @param exactIP
	 * 	The IP address to ban.
	 * @param message
	 * 	The message to use as a ban message; is also sent to all players who are online and caught in the ban.
	 * @param banEnd
	 * 	When does the ban end?
	 * @param adminBan
	 *  Was this an administrative ban?
	 * @param includeHistoric
	 *  Ban everyone who has ever used this IP address?
	 * @return
	 *  A BanResult object describing who was banned.
	 */
	public static BanResult doIPBan(BSIP exactIP, String message, Date banEnd, boolean adminBan, boolean includeHistoric) {
		try {
			if (message == null || message.trim().equals("")) {
				message = adminBan ? "Administrative Ban" : "Automatic Ban"; // TODO: config!
			}
			BSBan ban = BSBan.create(exactIP, message, banEnd, adminBan); // general ban.
			BanResult result = new BanResult();
			result.addBan(ban);
			
			for (Player player : Bukkit.getOnlinePlayers()) {
				BSPlayer banPlayer = BSPlayer.byUUID(player.getUniqueId());
				BSSession active = banPlayer.getLatestSession();
				if (active.getIP().getId() == exactIP.getId() && banPlayer.getBan() == null) { // TODO replace with equality check.
					banPlayer.setBan(ban);
					result.addPlayer(banPlayer);
					if (banEnd != null) {
						player.kickPlayer(message + ". Ends " + BanHandler.endTimeFormat.format(banEnd));
					} else {
						player.kickPlayer(message);
					}
				}
			}
				
			if (includeHistoric) {
				// also search Sessions, and ban everyone who ever used it.
				// TODO
			}
			
			return result;
		} catch (Exception e) {
			BanStick.getPlugin().warning("Failed to issue IP ban: ", e);
			return new BanResult();
		}
	}
	
	/**
	 * Does a ban against a CIDR range.
	 * 
	 * @param cidrIP
	 * 	cidr IP range to ban
	 * @param message
	 *  Message to record as ban reason
	 * @param banEnd
	 *  The time to end the ban
	 * @param adminBan
	 *  Is this an administrative ban?
	 * @param includeHistoric
	 *  Should we include all historic occurances of this IP in the ban? (TODO)
	 * @return
	 */
	public static BanResult doCIDRBan(BSIP cidrIP, String message, Date banEnd, boolean adminBan, boolean includeHistoric) {
		try {
			if (message == null || message.trim().equals("")) {
				message = adminBan ? "Administrative Ban" : "Automatic Ban"; // TODO: config!
			}
			BSBan ban = BSBan.create(cidrIP, message, banEnd, adminBan); // general ban.
			BanResult result = new BanResult();
			result.addBan(ban);
			
			for (Player player : Bukkit.getOnlinePlayers()) {
				BSPlayer banPlayer = BSPlayer.byUUID(player.getUniqueId());
				if (banPlayer.getBan() != null) continue; // already banned.
				
				BSSession active = banPlayer.getLatestSession();
				BSIP activeIP = active.getIP();
				boolean doBan = false;
				if (cidrIP.getIPv4Address() != null && activeIP.getIPv4Address() != null) {
					// check IPv4
					if (cidrIP.getIPv4Address().contains(activeIP.getIPv4Address())) {
						doBan = true;
					}
				} else if (cidrIP.getIPv6Address() != null && activeIP.getIPv6Address() != null) {
					if (cidrIP.getIPv6Address().contains(activeIP.getIPv6Address())) {
						doBan = true;
					}
				} // if mismatched, don't ban.
				
				if (doBan) {
					banPlayer.setBan(ban);
					result.addPlayer(banPlayer);
					if (banEnd != null) {
						player.kickPlayer(message + ". Ends " + BanHandler.endTimeFormat.format(banEnd));
					} else {
						player.kickPlayer(message);
					}
				}
			}
				
			if (includeHistoric) {
				// also search Sessions, and ban everyone who ever used it.
				// TODO
			}
			
			return result;
		} catch (Exception e) {
			BanStick.getPlugin().warning("Failed to issue CIDR ban: ", e);
			return new BanResult();
		}	
	}
}