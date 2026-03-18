import java.awt.image.BufferedImage;

public class EnemyAnimation {
    public BufferedImage[] idle;
    public BufferedImage[] attack;
    public BufferedImage[] hurt;
    public BufferedImage[] death;

    private BufferedImage[] currentAnimation;
    public int currentFrame = 0;
    public int frameCounter = 0;

    // Πιο αργό timing για να φαίνονται καλύτερα
    public final int FRAME_DELAY_IDLE = 18;
    public final int FRAME_DELAY_ACTION = 11;

    public boolean isPlaying = false;
    public boolean loop = true;
    public String currentAnimName = "idle";

    // Χρησιμοποιείται από το battle system για να ξέρει ότι ένα one-shot animation τελείωσε
    public boolean actionJustFinished = false;

    public EnemyAnimation(BufferedImage[] idleFrames,
                          BufferedImage[] attackFrames,
                          BufferedImage[] hurtFrames,
                          BufferedImage[] deathFrames) {
        this.idle = idleFrames;
        this.attack = attackFrames;
        this.hurt = hurtFrames;
        this.death = deathFrames;

        this.currentAnimation = idleFrames;
        this.currentAnimName = "idle";
        this.currentFrame = 0;
        this.frameCounter = 0;
        this.isPlaying = true;
        this.loop = true;
        this.actionJustFinished = false;
    }

    public void update() {
        if (currentAnimation == null || currentAnimation.length == 0) return;
        if (!isPlaying) return;

        frameCounter++;

        int currentDelay = FRAME_DELAY_IDLE;
        if (currentAnimName.equals("attack") ||
            currentAnimName.equals("hurt") ||
            currentAnimName.equals("death")) {
            currentDelay = FRAME_DELAY_ACTION;
        }

        if (frameCounter >= currentDelay) {
            frameCounter = 0;
            currentFrame++;

            if (currentFrame >= currentAnimation.length) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    // One-shot animations
                    if (currentAnimName.equals("death")) {
                        // Μείνε στο τελευταίο frame του death
                        currentFrame = currentAnimation.length - 1;
                        isPlaying = false;
                        actionJustFinished = true;
                    } else {
                        // attack / hurt τελείωσαν -> γύρνα σε idle αλλά συνέχισε να παίζεις
                        currentAnimation = idle;
                        currentAnimName = "idle";
                        currentFrame = 0;
                        frameCounter = 0;
                        loop = true;
                        isPlaying = true;
                        actionJustFinished = true;
                    }
                }
            }
        }
    }

    public BufferedImage getCurrentImage() {
        if (currentAnimation != null &&
            currentAnimation.length > 0 &&
            currentFrame >= 0 &&
            currentFrame < currentAnimation.length) {
            return currentAnimation[currentFrame];
        }

        if (idle != null && idle.length > 0) {
            return idle[0];
        }

        return null;
    }

    public void setAnimation(String animName, boolean shouldLoop) {
        // Μην ξαναφορτώνεις το ίδιο animation ενώ ήδη παίζει
        if (animName.equals(currentAnimName) && isPlaying) return;

        BufferedImage[] selectedAnimation = null;

        switch (animName) {
            case "attack":
                selectedAnimation = attack;
                break;
            case "hurt":
                selectedAnimation = hurt;
                break;
            case "death":
                selectedAnimation = death;
                shouldLoop = false;
                break;
            case "idle":
            default:
                selectedAnimation = idle;
                animName = "idle";
                break;
        }

        // Fallback αν κάτι λείπει
        if (selectedAnimation == null || selectedAnimation.length == 0) {
            selectedAnimation = idle;
            animName = "idle";
            shouldLoop = true;
        }

        currentAnimation = selectedAnimation;
        currentAnimName = animName;
        currentFrame = 0;
        frameCounter = 0;
        loop = shouldLoop;
        isPlaying = true;
        actionJustFinished = false;
    }

    public boolean isFinished() {
        return actionJustFinished;
    }

    public int getStrikeFrame() {
        // Για 8-frame attack, συνήθως καλύτερα στο 4
        if (currentAnimName.equals("attack")) return 4;
        return -1;
    }

    public boolean isOnStrikeFrame() {
        return currentFrame == getStrikeFrame();
    }
}
