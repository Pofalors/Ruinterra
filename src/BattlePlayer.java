import java.awt.image.BufferedImage;

public class BattlePlayer {
    public Entity player;        
    public double x, y;
    public double targetX, targetY;
    public int hp, mp;
    public int maxHp, maxMp;
    public BufferedImage image;  // Fallback image
    
    // ΝΕΟ: Animation για τη μάχη
    public PlayerAnimation anim;
    
    // Μέθοδος για να παίζει animation
    public void playAnimation(String name) {
        if (anim != null) {
            anim.setAnimation(name, name.equals("idle"));
        }
    }
    
    // Getter για την τρέχουσα εικόνα μάχης
    public BufferedImage getCurrentImage() {
        if (anim != null && anim.isPlaying) {
            return anim.getCurrentImage();
        }
        return image; // fallback στην παλιά εικόνα
    }
    
    // Ενημέρωση animation (καλείται κάθε frame)
    public void update() {
        if (anim != null) {
            anim.update();
        }
    }
}
