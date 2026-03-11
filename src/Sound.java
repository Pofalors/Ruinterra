import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;

public class Sound {
    private HashMap<String, Clip> soundEffects = new HashMap<>();
    private HashMap<String, Clip> musicTracks = new HashMap<>();
    private String currentMusic = "";
    
    public void setFile(String soundFile) {
        try {
            File file = new File("res/sound/" + soundFile);
            System.out.println("Trying to load: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());
            
            if (!file.exists()) {
                System.out.println("Sound file not found: " + soundFile);
                return;
            }
            
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            
            // Για συμβατότητα με την παλιά μέθοδο
            this.clip = clip;
        } catch (Exception e) {
            System.out.println("Error loading sound: " + soundFile);
            e.printStackTrace();
        }
    }
    
    // Για συμβατότητα με την παλιά μέθοδο
    private Clip clip;
    
    public void play() {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    public void loop() {
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }
    
    public void stop() {
        if (clip != null) {
            clip.stop();
        }
    }
    
    public void preloadSound(String name, String filename) {
        try {
            File file = new File("res/sound/" + filename);
            System.out.println("Preloading sound: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());
            
            if (!file.exists()) {
                System.out.println("❌ Sound file NOT FOUND: " + filename);
                return;
            }
            
            // Απλά αποθήκευσε το filename, όχι το Clip
            soundEffects.put(name, null);
            System.out.println("✅ Registered sound: " + name + " from " + filename);
        } catch (Exception e) {
            System.out.println("❌ Error registering sound: " + filename);
        }
    }
    
    public void preloadMusic(String name, String filename) {
        try {
            File file = new File("res/sound/" + filename);
            System.out.println("Preloading music: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());
            
            if (!file.exists()) {
                System.out.println("❌ Music file NOT FOUND: " + filename);
                return;
            }
            
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip newClip = AudioSystem.getClip();
            newClip.open(ais);
            musicTracks.put(name, newClip);
            System.out.println("✅ Preloaded music: " + name + " from " + filename);
        } catch (Exception e) {
            System.out.println("❌ Error preloading music: " + filename);
            e.printStackTrace();
        }
    }

    // Στο Sound.java, ΠΡΟΣΘΕΣΕ αυτή τη μέθοδο (μόνο μία φορά):
    public void setMusicVolume(float volume) {
        // volume: 0.0 - 1.0
        for (Clip clip : musicTracks.values()) {
            if (clip != null && clip.isRunning()) {
                try {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB;
                    if (volume <= 0.0f) {
                        dB = gainControl.getMinimum();
                    } else {
                        dB = (float)(Math.log(volume) / Math.log(10.0) * 20.0);
                    }
                    // Περιόρισε σε λογικά όρια
                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    gainControl.setValue(dB);
                } catch (Exception e) {
                    // Αν δεν υποστηρίζεται volume control, απλά αγνόησε
                }
            }
        }
    }

    // ΑΝΤΙΚΑΤΕΣΤΗΣΕ την υπάρχουσα setMusicVolume(int volume) με αυτή:
    public void setMusicVolume(int volume) {
        setMusicVolume(volume / 100.0f);
    }
    
    public void playSE(String name) {
        try {
            // Βρες το αρχείο από το όνομα
            String filename = name + ".wav";
            File file = new File("res/sound/" + filename);
            
            if (!file.exists()) {
                System.out.println("❌ Sound file not found: " + filename);
                return;
            }
            
            // Δημιούργησε νέο clip κάθε φορά
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip newClip = AudioSystem.getClip();
            newClip.open(ais);
            
            // Αυτόματο κλείσιμο όταν τελειώσει
            newClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    newClip.close();
                }
            });
            
            newClip.start();
            System.out.println("▶ Playing sound: " + name);
            
        } catch (Exception e) {
            System.out.println("❌ Error playing sound: " + name);
        }
    }
    
    public void playMusic(String name) {
        if (!currentMusic.isEmpty()) {
            Clip oldMusic = musicTracks.get(currentMusic);
            if (oldMusic != null && oldMusic.isRunning()) {
                oldMusic.stop();
                System.out.println("⏹ Stopped music: " + currentMusic);
            }
        }
        
        Clip newMusic = musicTracks.get(name);
        if (newMusic != null) {
            newMusic.setFramePosition(0);
            newMusic.loop(Clip.LOOP_CONTINUOUSLY);
            currentMusic = name;
            System.out.println("▶ Playing music: " + name);
        } else {
            System.out.println("❌ Music not found: " + name);
        }
    }
    
    public void stopMusic() {
        if (!currentMusic.isEmpty()) {
            Clip music = musicTracks.get(currentMusic);
            if (music != null && music.isRunning()) {
                music.stop();
                System.out.println("⏹ Stopped music: " + currentMusic);
            }
            currentMusic = "";
        }
    }
    
    public boolean isMusicPlaying() {
        return !currentMusic.isEmpty();
    }
    
    public String getCurrentMusic() {
        return currentMusic;
    }
}