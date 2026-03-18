import java.awt.image.BufferedImage;

public class PlayerAnimation {
    public BufferedImage[] idle;
    public BufferedImage[] hurt;
    public BufferedImage[] death;
    public BufferedImage[] attack1;
    public BufferedImage[] attack2;
    public BufferedImage[] attack3;
    public BufferedImage[] lowHpIdle;
    public BufferedImage[] defend;
    public BufferedImage[] useItem;

    private BufferedImage[] currentAnimation;
    public int currentFrame = 0;
    public int frameCounter = 0;
    public boolean isPlaying = false;
    public boolean loop = true;
    public String currentAnimName = "idle";
    public boolean actionJustFinished = false;

    public final int FRAME_DELAY_NORMAL = 15;
    public final int FRAME_DELAY_FAST = 10;

    public PlayerAnimation(BufferedImage[] idleFrames,
                           BufferedImage[] hurtFrames,
                           BufferedImage[] deathFrames,
                           BufferedImage[] attack1Frames) {

        this.idle = idleFrames;
        this.hurt = hurtFrames;
        this.death = deathFrames;
        this.attack1 = attack1Frames;

        this.attack2 = null;
        this.attack3 = null;
        this.lowHpIdle = null;
        this.defend = null;
        this.useItem = null;

        this.currentAnimation = idleFrames;
        this.isPlaying = true;
        this.loop = true;
    }

    public void update() {
        if (!isPlaying || currentAnimation == null || currentAnimation.length == 0) return;

        frameCounter++;

        int currentDelay = FRAME_DELAY_NORMAL;
        if (currentAnimName.equals("attack1") ||
            currentAnimName.equals("attack2") ||
            currentAnimName.equals("attack3") ||
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
                    if (currentAnimName.equals("death")) {
                        isPlaying = false;
                        currentFrame = currentAnimation.length - 1;
                        actionJustFinished = true;
                    } else {
                        currentAnimation = idle;
                        currentAnimName = "idle";
                        currentFrame = 0;
                        frameCounter = 0;
                        loop = true;
                        isPlaying = true;   // ΣΗΜΑΝΤΙΚΟ: idle συνεχίζει κανονικά
                        actionJustFinished = true;
                    }
                }
            }
        }
    }

    public BufferedImage getCurrentImage() {
        if (currentAnimation != null && currentAnimation.length > 0 && currentFrame < currentAnimation.length) {
            return currentAnimation[currentFrame];
        }

        if (idle != null && idle.length > 0) {
            return idle[0];
        }

        return null;
    }

    public void setAnimation(String animName, boolean shouldLoop) {
        if (animName.equals(currentAnimName) && isPlaying) return;

        BufferedImage[] selected = null;

        switch (animName) {
            case "idle":
                selected = idle;
                break;
            case "hurt":
                selected = hurt;
                break;
            case "death":
                selected = death;
                shouldLoop = false;
                break;
            case "attack1":
                selected = attack1;
                break;
            case "attack2":
                selected = (attack2 != null) ? attack2 : attack1;
                break;
            case "attack3":
                selected = (attack3 != null) ? attack3 : ((attack2 != null) ? attack2 : attack1);
                break;
            case "lowHpIdle":
                selected = (lowHpIdle != null) ? lowHpIdle : idle;
                break;
            case "defend":
                selected = (defend != null) ? defend : idle;
                break;
            case "useItem":
                selected = (useItem != null) ? useItem : attack1;
                break;
            default:
                selected = idle;
                animName = "idle";
                shouldLoop = true;
                break;
        }

        if (selected == null || selected.length == 0) {
            selected = idle;
            animName = "idle";
            shouldLoop = true;
        }

        currentAnimName = animName;
        loop = shouldLoop;
        currentFrame = 0;
        frameCounter = 0;
        isPlaying = true;
        actionJustFinished = false;
        currentAnimation = selected;
    }

    public boolean isFinished() {
        return actionJustFinished;
    }

    public int getStrikeFrame() {
        switch (currentAnimName) {
            case "attack1":
                return 2;
            case "attack2":
                return 3;
            case "attack3":
                return 4;
            case "useItem":
                return 2;
            default:
                return -1;
        }
    }

    public boolean isOnStrikeFrame() {
        return currentFrame == getStrikeFrame();
    }
}