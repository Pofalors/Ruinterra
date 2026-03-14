import java.awt.image.BufferedImage;

public class EnemyAnimation {
    public BufferedImage[] idle;
    public BufferedImage[] attack;  // 8 frames
    public BufferedImage[] hurt;
    public BufferedImage[] death;
    
    private BufferedImage[] currentAnimation;
    public int currentFrame = 0;
    public int frameCounter = 0;
    public final int FRAME_DELAY = 8; // Ταχύτητα animation
    
    public boolean isPlaying = false;
    public boolean loop = true;
    public String currentAnimName = "idle";
    
    public EnemyAnimation(BufferedImage[] idleFrames, BufferedImage[] attackFrames,
                          BufferedImage[] hurtFrames, BufferedImage[] deathFrames) {
        this.idle = idleFrames;
        this.attack = attackFrames;
        this.hurt = hurtFrames;
        this.death = deathFrames;
        this.currentAnimation = idleFrames;
    }
    
    public void update() {
        if (!isPlaying) return;
        
        frameCounter++;
        if (frameCounter >= FRAME_DELAY) {
            frameCounter = 0;
            currentFrame++;
            
            if (currentFrame >= currentAnimation.length) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    isPlaying = false;
                    // Μετά από attack/hurt, γύρνα στο idle
                    if (currentAnimName.equals("attack") || currentAnimName.equals("hurt")) {
                        setAnimation("idle", true);
                    }
                }
            }
        }
    }
    
    public BufferedImage getCurrentImage() {
        if (currentAnimation != null && currentFrame < currentAnimation.length) {
            return currentAnimation[currentFrame];
        }
        return idle[0];
    }
    
    public void setAnimation(String animName, boolean shouldLoop) {
        if (animName.equals(currentAnimName) && isPlaying) return;
        
        currentAnimName = animName;
        loop = shouldLoop;
        currentFrame = 0;
        frameCounter = 0;
        isPlaying = true;
        
        switch(animName) {
            case "attack":
                currentAnimation = attack;
                break;
            case "hurt":
                currentAnimation = hurt;
                break;
            case "death":
                currentAnimation = death;
                break;
            default:
                currentAnimation = idle;
                break;
        }
    }
}
