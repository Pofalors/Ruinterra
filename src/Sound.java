import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;

public class Sound {
    private HashMap<String, Clip> soundEffects = new HashMap<>();
    private HashMap<String, Clip> musicTracks = new HashMap<>();
    private String currentMusic = "";
    private Clip currentBattleLoopClip = null;
    private String currentBattleLoopName = "";

    // Base folders
    private static final String SOUND_FOLDER = "res/sound/";
    private static final String BATTLE_SOUND_FOLDER = "res/sound/battle/";

    // Για συμβατότητα με την παλιά μέθοδο
    private Clip clip;

    public void setFile(String soundFile) {
        try {
            File file = new File(SOUND_FOLDER + soundFile);
            System.out.println("Trying to load: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());

            if (!file.exists()) {
                System.out.println("Sound file not found: " + soundFile);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);

            this.clip = clip;
        } catch (Exception e) {
            System.out.println("Error loading sound: " + soundFile);
            e.printStackTrace();
        }
    }

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

    // =========================
    // NORMAL SOUND EFFECTS
    // =========================
    public void preloadSound(String name, String filename) {
        try {
            File file = new File(SOUND_FOLDER + filename);
            System.out.println("Preloading sound: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());

            if (!file.exists()) {
                System.out.println("❌ Sound file NOT FOUND: " + filename);
                return;
            }

            soundEffects.put(name, null);
            System.out.println("✅ Registered sound: " + name + " from " + filename);
        } catch (Exception e) {
            System.out.println("❌ Error registering sound: " + filename);
        }
    }

    public void playSE(String name) {
        try {
            String filename = name + ".wav";
            File file = new File(SOUND_FOLDER + filename);

            if (!file.exists()) {
                System.out.println("❌ Sound file not found: " + filename);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip newClip = AudioSystem.getClip();
            newClip.open(ais);

            newClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    newClip.close();
                }
            });

            newClip.start();
            System.out.println("▶ Playing sound: " + name);

        } catch (Exception e) {
            System.out.println("❌ Error playing sound: " + name);
            e.printStackTrace();
        }
    }

    // =========================
    // BATTLE SOUND EFFECTS
    // =========================
    public void preloadBattleSound(String name, String filename) {
        try {
            File file = new File(BATTLE_SOUND_FOLDER + filename);
            System.out.println("Preloading battle sound: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());

            if (!file.exists()) {
                System.out.println("❌ Battle sound file NOT FOUND: " + filename);
                return;
            }

            soundEffects.put(name, null);
            System.out.println("✅ Registered battle sound: " + name + " from " + filename);
        } catch (Exception e) {
            System.out.println("❌ Error registering battle sound: " + filename);
            e.printStackTrace();
        }
    }

    public void playBattleSE(String name) {
        try {
            String filename = name + ".wav";
            File file = new File(BATTLE_SOUND_FOLDER + filename);

            if (!file.exists()) {
                System.out.println("❌ Battle sound file not found: " + filename);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip newClip = AudioSystem.getClip();
            newClip.open(ais);

            newClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    newClip.close();
                }
            });

            newClip.start();
            System.out.println("▶ Playing battle sound: " + name);

        } catch (Exception e) {
            System.out.println("❌ Error playing battle sound: " + name);
            e.printStackTrace();
        }
    }

    public long getBattleSoundLengthMs(String name) {
        try {
            String filename = name + ".wav";
            File file = new File(BATTLE_SOUND_FOLDER + filename);

            if (!file.exists()) {
                return 0;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            AudioFormat format = ais.getFormat();
            long frames = ais.getFrameLength();
            ais.close();

            return (long)((frames * 1000.0) / format.getFrameRate());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void playBattleLoopFromMs(String name, int loopStartMs) {
        stopBattleLoop();

        try {
            String filename = "res/sound/battle/" + name + ".wav";
            File file = new File(filename);

            if (!file.exists()) {
                System.out.println("❌ Battle loop file not found: " + filename);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);

            currentBattleLoopClip = clip;
            currentBattleLoopName = name;

            // Παίξε μία φορά από την αρχή
            clip.setFramePosition(0);
            clip.start();

            // Όταν φτάσει στο τέλος, ξαναπήγαινε από το loopStartMs και μετά
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && currentBattleLoopClip == clip) {
                    if (clip.getMicrosecondPosition() >= clip.getMicrosecondLength() - 2000) {
                        long startMicros = loopStartMs * 1000L;
                        clip.setMicrosecondPosition(startMicros);
                        clip.start();
                    }
                }
            });

            System.out.println("▶ Playing battle loop: " + name + " (loop from " + loopStartMs + " ms)");

        } catch (Exception e) {
            System.out.println("❌ Error playing battle loop: " + name);
            e.printStackTrace();
        }
    }

    public void stopBattleLoop() {
        if (currentBattleLoopClip != null) {
            try {
                currentBattleLoopClip.stop();
                currentBattleLoopClip.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentBattleLoopClip = null;
            currentBattleLoopName = "";
        }
    }

    public boolean isBattleLoopPlaying(String name) {
        return currentBattleLoopClip != null &&
            currentBattleLoopClip.isRunning() &&
            currentBattleLoopName.equals(name);
    }

    // =========================
    // MUSIC
    // =========================
    public void preloadMusic(String name, String filename) {
        try {
            File file = new File(SOUND_FOLDER + filename);
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

    public void setMusicVolume(float volume) {
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

                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    gainControl.setValue(dB);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    public void setMusicVolume(int volume) {
        setMusicVolume(volume / 100.0f);
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