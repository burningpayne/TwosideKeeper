package sig.plugin.TwosideKeeper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class AutoUpdatePlugin implements Runnable {
	List<Plugin> plugins;
	boolean restarting=false;
	org.bukkit.plugin.Plugin plug=null;
	
	public AutoUpdatePlugin(org.bukkit.plugin.Plugin plug) {
		plugins = new ArrayList<Plugin>();
		this.plug=plug;
	}
	
	@Override
	public void run() {
		try {
			FetchPlugins();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void FetchPlugins() throws IOException {
		for (int i=0;i<plugins.size();i++) {
			FileUtils.copyURLToFile(new URL(plugins.get(i).url), new File(TwosideKeeper.filesave,"updates/"+plugins.get(i).name));
			
			//After that's done, check the hash.
			FileInputStream file = new FileInputStream(new File(TwosideKeeper.filesave,"updates/"+plugins.get(i).name));
			String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(file);
			file.close();
			
			if (plugins.get(i).hash==null || !md5.equalsIgnoreCase(plugins.get(i).hash)) {
				//This plugin is different! Update the hash for it. Prepare for a restart of the server!
				restarting=true;
				//Save the new plugin hash.
				plugins.get(i).hash = md5;
				SaveHash(plugins.get(i));
				Bukkit.broadcastMessage("The server has detected a new version of "+ChatColor.YELLOW+plugins.get(i).name+". The server will restart in 3 minutes!");
				//Move the file to the new location.
				FileUtils.copyFile(new File(TwosideKeeper.filesave,"updates/"+plugins.get(i).name),
						new File(TwosideKeeper.filesave,"../"+plugins.get(i).name+".jar"));
			}
		}
		if (restarting) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(plug, new Runnable() {
				@Override
				public void run() {
					Bukkit.broadcastMessage(ChatColor.YELLOW+"The server is restarting in 1 minute for a plugin update!");
				}
			},20*120);
			Bukkit.getScheduler().scheduleSyncDelayedTask(plug, new Runnable() {
				@Override
				public void run() {
					Bukkit.broadcastMessage(ChatColor.RED+"The server is restarting in 10 seconds!");
				}
			},20*170);
			Bukkit.getScheduler().scheduleSyncDelayedTask(plug, new Runnable() {
				@Override
				public void run() {
					Bukkit.savePlayers();
					for (int i=0;i<Bukkit.getWorlds().size();i++) {
						Bukkit.getWorlds().get(i).save();
					}
					Bukkit.shutdown();
				}
			},20*180);
		}
	}
	
	public void AddPlugin(String name, String url) {
		plugins.add(new Plugin(name,url));
	}
	
	public void LoadHash(Plugin pluginname) {
		//Read from the server config.
		File config = new File(TwosideKeeper.filesave,"hashes.data");
		FileConfiguration workable = YamlConfiguration.loadConfiguration(config);
		pluginname.hash = workable.getString(pluginname.name+"/HASH");
	}
	public void SaveHash(Plugin pluginname) {
		File config = new File(TwosideKeeper.filesave,"hashes.data");
		FileConfiguration workable = YamlConfiguration.loadConfiguration(config);
		workable.set(pluginname.name+"/HASH",pluginname.hash);
		try {
			workable.save(config);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void SaveAllHashes(List<Plugin> pluginlist) {
		File config = new File(TwosideKeeper.filesave,"hashes.data");
		FileConfiguration workable = YamlConfiguration.loadConfiguration(config);
		for (int i=0;i<pluginlist.size();i++) {
			workable.set(pluginlist.get(i).name+"/HASH",pluginlist.get(i).hash);
		}
		try {
			workable.save(config);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class Plugin {
	String name;
	String hash;
	String url;
	
	public Plugin(String name,String url) {
		this.name=name;
		this.url=url;
		this.hash=FetchHash(); //Try to fetch the hash.
	}
	
	public Plugin(String name,String hash,String url) {
		this.name=name;
		this.url=url;
		this.hash=hash;
	}
	
	public String FetchHash() {
		File config = new File(TwosideKeeper.filesave,"hashes.data");
		FileConfiguration workable = YamlConfiguration.loadConfiguration(config);
		return workable.getString(this.name+"/HASH");
	}
}
