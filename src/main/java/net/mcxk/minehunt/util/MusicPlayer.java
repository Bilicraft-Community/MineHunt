package net.mcxk.minehunt.util;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import lombok.SneakyThrows;
import net.mcxk.minehunt.MineHunt;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.nio.file.Files;

public class MusicPlayer {
    @SneakyThrows
    public void playEnding(){
//        if(Bukkit.getPluginManager().isPluginEnabled("NoteBlockAPI")) {
//          //  Files.copy(MineHunt.getInstance().getResource("newNBS.nbs"),new File(MineHunt.getInstance().getDataFolder(),"nbs.nbs").toPath());
//            Song song = NBSDecoder.parse(MineHunt.getInstance().getResource("newNBS.nbs"));
//            if(song == null){
//                MineHunt.getInstance().getLogger().warning("Cannot play NBS song cause it is null.");
//                return;
//            }
//            RadioSongPlayer rsp = new RadioSongPlayer(song);
//            Bukkit.getOnlinePlayers().forEach(rsp::addPlayer);
//            rsp.setPlaying(true);
//        }
//        Bukkit.getOnlinePlayers().forEach(p->{
//            if(p.isDead()){p.spigot().respawn();}
//        });
        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(p->p.playSound(p.getLocation(), Sound.MUSIC_DISC_WAIT,1.0f,1.0f));
            }
        }.runTaskLater(MineHunt.getInstance(),1);
    }
}
