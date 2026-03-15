import java.awt.image.BufferedImage;

public class EnemyAnimation {
    public BufferedImage[] idle;
    public BufferedImage[] attack;  // 8 frames
    public BufferedImage[] hurt;
    public BufferedImage[] death;
    
    private BufferedImage[] currentAnimation;
    public int currentFrame = 0;
    public int frameCounter = 0;
    public final int FRAME_DELAY_IDLE = 15;    // Αργό για idle (15 frames)
    public final int FRAME_DELAY_ACTION = 8;   // Γρήγορο για attack/hurt/death (5 frames)
    
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
        
        // Διάλεξε ταχύτητα ανάλογα με το animation
        int currentDelay = FRAME_DELAY_IDLE; // default
        
        if (currentAnimName.equals("attack") ||  
            currentAnimName.equals("death")) {
            currentDelay = FRAME_DELAY_ACTION; // Πιο γρήγορο
        }
        
        if (frameCounter >= currentDelay) {
            frameCounter = 0;
            currentFrame++;
            
            if (currentFrame >= currentAnimation.length) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    isPlaying = false;
                    // Μείνε στο τελευταίο frame για το death
                    if (currentAnimName.equals("death")) {
                        currentFrame = currentAnimation.length - 1;
                    } else if (currentAnimName.equals("attack") || currentAnimName.equals("hurt")) {
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
        // Αν το death animation τελείωσε, δείξε το τελευταίο frame
        if (currentAnimName.equals("death") && !isPlaying && death.length > 0) {
            return death[death.length - 1];
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
                loop = false;
                break;
            default:
                currentAnimation = idle;
                break;
        }
    }
}
