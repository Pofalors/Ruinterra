import java.awt.image.BufferedImage;

public class PlayerAnimation {
    // Τα διαφορετικά animations
    public BufferedImage[] idle;        // 3 frames
    public BufferedImage[] hurt;        // 2 frames
    public BufferedImage[] death;       // 3 frames
    public BufferedImage[] attack1;     // Attack variation 1
    public BufferedImage[] attackAxe;   // Attack variation 2
    public BufferedImage[] attack02;    // Attack variation 3
    public BufferedImage[] lowHpIdle;   // 4 frames
    public BufferedImage[] defend;      // 1 frame
    public BufferedImage[] useItem;     // 5 frames
    
    // Τρέχον animation
    private BufferedImage[] currentAnimation;
    public int currentFrame = 0;
    public int frameCounter = 0;
    public boolean isPlaying = false;
    public boolean loop = true;
    public String currentAnimName = "idle";
    
    // Ταχύτητες animation (μπορείς να τις προσαρμόσεις)
    public final int FRAME_DELAY_NORMAL = 12;   // για idle, lowHpIdle
    public final int FRAME_DELAY_FAST = 6;      // για attack, hurt, death
    
    public PlayerAnimation(BufferedImage[] idleFrames,
                        BufferedImage[] hurtFrames,
                        BufferedImage[] deathFrames,
                        BufferedImage[] attack1Frames) {
        
        this.idle = idleFrames;
        this.hurt = hurtFrames;
        this.death = deathFrames;
        this.attack1 = attack1Frames;
        
        // Τα υπόλοιπα animations τα αφήνουμε null προσωρινά
        this.attackAxe = null;
        this.attack02 = null;
        this.lowHpIdle = null;
        this.defend = null;
        this.useItem = null;
        
        // Ξεκίνα με idle
        this.currentAnimation = idleFrames;
    }
    
    public void update() {
        if (!isPlaying) return;
        
        frameCounter++;
        
        // Διάλεξε ταχύτητα ανάλογα με το animation
        int currentDelay = FRAME_DELAY_NORMAL;
        if (currentAnimName.equals("attack1") || 
            currentAnimName.equals("attackAxe") ||
            currentAnimName.equals("attack02") ||
            currentAnimName.equals("hurt") ||
            currentAnimName.equals("death") ||
            currentAnimName.equals("useItem")) {
            currentDelay = FRAME_DELAY_FAST;
        }
        
        if (frameCounter >= currentDelay) {
            frameCounter = 0;
            currentFrame++;
            
            if (currentFrame >= currentAnimation.length) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    isPlaying = false;
                    // Για death, μείνε στο τελευταίο frame
                    if (currentAnimName.equals("death")) {
                        currentFrame = currentAnimation.length - 1;
                    } else {
                        // Για άλλα non-looping animations, γύρνα στο idle
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
        // Fallback
        if (idle != null && idle.length > 0) {
            return idle[0];
        }
        return null;
    }
    
    public void setAnimation(String animName, boolean shouldLoop) {
        // Αν είναι το ίδιο animation και παίζει ήδη, μην το ξαναρχίσεις
        if (animName.equals(currentAnimName) && isPlaying) return;
        
        currentAnimName = animName;
        loop = shouldLoop;
        currentFrame = 0;
        frameCounter = 0;
        isPlaying = true;
        
        switch(animName) {
            case "idle":
                currentAnimation = idle;
                break;
            case "hurt":
                currentAnimation = hurt;
                break;
            case "death":
                currentAnimation = death;
                loop = false;
                break;
            case "attack1":
                currentAnimation = attack1;
                break;
            case "attackAxe":
                currentAnimation = attackAxe;
                break;
            case "attack02":
                currentAnimation = attack02;
                break;
            case "lowHpIdle":
                currentAnimation = lowHpIdle;
                break;
            case "defend":
                currentAnimation = defend;
                break;
            case "useItem":
                currentAnimation = useItem;
                break;
            default:
                currentAnimation = idle;
                break;
        }
    }
}