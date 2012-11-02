/*
 * This file is part of MineQuest-Idle, Idle World implementation for MineQuest.
 * MineQuest-Idle is licensed under GNU General Public License v3.
 * Copyright (C) 2012 The MineQuest Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.theminequest.idle;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.theminequest.MineQuest.API.Managers;
import com.theminequest.MineQuest.API.Quest.Quest;
import com.theminequest.MineQuest.API.Quest.QuestDetails;
import com.theminequest.MineQuest.API.Utils.PropertiesFile;

public class Main extends JavaPlugin implements Listener {
	
	public static final String PROPERTIES_NAME = "idle.properties";
	private PropertiesFile properties;
	private LinkedHashMap<String,List<Quest>> quests;
	
	@Override
	public void onDisable() {
		
	}
	
	@Override
	public void onEnable() {
		if (getServer().getPluginManager().getPlugin("MineQuest") == null) {
			getServer().getLogger().severe("============= MineQuest-Idle =============");
			getServer().getLogger().severe("MineQuest is required for Idle to operate!");
			getServer().getLogger().severe("Please install MineQuest first!");
			getServer().getLogger().severe("You can find the latest version here:");
			getServer().getLogger().severe("http://dev.bukkit.org/server-mods/minequest/");
			getServer().getLogger().severe("==============================================");
			setEnabled(false);
			return;
		}
		properties = new PropertiesFile(Managers.getActivePlugin().getDataFolder().getAbsolutePath()+File.separator+PROPERTIES_NAME);
		for (World w : Bukkit.getWorlds()){
			properties.getString(w.getName(), "/");
		}
		quests = new LinkedHashMap<String, List<Quest>>();
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e){
		if (e.getPlayer().getWorld().getName().startsWith("mqinstance_"))
			return;
		String questName = properties.getString(e.getPlayer().getWorld().getName(), "/");
		if (questName.equals("/"))
			return;
		tryStart(e.getPlayer().getName(),questName);

	}
	
	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent e){
		if (e.getPlayer().getWorld().getName().startsWith("mqinstance_"))
			return;
//		String stopquest = properties.getString(e.getFrom().getName(),"/");
//		if (!stopquest.equals("/")) {
//			Quest q = Managers.getQuestManager().getMainWorldQuest(e.getPlayer().getName(), stopquest);
//			if (q != null)
//				q.finishQuest(CompleteStatus.CANCELED);
//		}
		String questName = properties.getString(e.getPlayer().getWorld().getName(), "/");
		if (questName.equals("/"))
			return;
		
		tryStart(e.getPlayer().getName(),questName);
	}
	
	private void tryStart(String playerName, String questName) {
		if (!quests.containsKey(playerName))
			quests.put(playerName,new ArrayList<Quest>());
		
		if (!hasQuest(playerName,questName)) {
			QuestDetails d = Managers.getQuestManager().getDetails(questName);
			if (d!=null) {
				try {
					Method m = com.theminequest.MineQuest.Quest.Quest.class.getMethod("newInstance", long.class, QuestDetails.class, String.class);
					m.setAccessible(true);
					Quest q = (Quest) m.invoke(null, -1,d,playerName);
					quests.get(playerName).add(q);
					q.startQuest();
				} catch (Exception ex) {
					Managers.log(Level.SEVERE, "[Idle] Could not start Idle Background Quest!");
					ex.printStackTrace();
				}
			}
		}
	}
	
	private boolean hasQuest(String playerName, String questName) {
		for (Quest q : quests.get(playerName)) {
			if (q.getDetails().getProperty(QuestDetails.QUEST_NAME).equals(questName))
				return true;
		}
		return false;
	}
	
}