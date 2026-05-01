import java.awt.image.BufferedImage;

public class PlayerAnimation {
    public BufferedImage[] idle;
    public BufferedImage[] hurt;
    public BufferedImage[] death;
    public BufferedImage[] attack1;
    public BufferedImage[] attack2;
    public BufferedImage[] attack3;
    public BufferedImage[] attack4;
    public BufferedImage[] absorb;
    public BufferedImage[] poison;
    public BufferedImage[] beforeCast;
    public BufferedImage[] attackMagic;
    public BufferedImage[] lowHpIdle;
    public BufferedImage[] defend;
    public BufferedImage[] useItem;
    public BufferedImage[] runLeft;
    public BufferedImage[] runRight;

    private BufferedImage[] currentAnimation;
    public int currentFrame = 0;
    public int frameCounter = 0;
    public boolean isPlaying = false;
    public boolean loop = true;
    public String currentAnimName = "idle";
    public boolean actionJustFinished = false;

    public final int FRAME_DELAY_NORMAL = 16;
    public final int FRAME_DELAY_FAST = 11;
    public final int FRAME_DELAY_SLOW = 30;

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
        this.attack4 = null;
        this.lowHpIdle = null;
        this.defend = null;
        this.useItem = null;
        this.absorb = null;
        this.poison = null;
        this.beforeCast = null;
        this.attackMagic = null;
        this.runLeft = null;
        this.runRight = null;

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
            currentAnimName.equals("useItem") ||
            currentAnimName.equals("run_left") ||
            currentAnimName.equals("run_right")) {
            currentDelay = FRAME_DELAY_FAST;
        } else if (currentAnimName.equals("beforeCast")) {
            currentDelay = FRAME_DELAY_SLOW;
            loop = true;
        } else if (currentAnimName.equals("absorb") ||
                currentAnimName.equals("poison")) {
            currentDelay = FRAME_DELAY_NORMAL;
        } else if (currentAnimName.equals("attackMagic")) {
            currentDelay = 22;
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
        if (animName.equals(currentAnimName) && isPlaying && !animName.equals("beforeCast")) return;

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
            case "attack4":
                selected = (attack4 != null) ? attack4 : 
                        ((attack3 != null) ? attack3 : 
                        ((attack2 != null) ? attack2 : attack1));
                break;
            case "absorb":
                selected = (absorb != null) ? absorb : attack1;
                break;
            case "poison":
                selected = (poison != null) ? poison : attack1;
                break;
            case "beforeCast":
                selected = (beforeCast != null) ? beforeCast : idle;
                shouldLoop = true;
                break;
            case "attackMagic":
                selected = (attackMagic != null) ? attackMagic : attack1;
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
            case "run_left":
                selected = (runLeft != null) ? runLeft : idle;
                break;
            case "run_right":
                selected = (runRight != null) ? runRight : idle;
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

    public int getCurrentFrameIndex() {
        return currentFrame;
    }

    public boolean isFinished() {
        return actionJustFinished;
    }

    public int getStrikeFrame() {
        switch (currentAnimName) {
            case "attack1":
                return 1;  // Hero: 1, Assassin: 2 — οπότε βάζουμε 1 (ο hero έχει πάντα μικρότερο, οι άλλοι έχουν +1)
            case "attack2":
                return 1;
            case "attack3":
                return 1;
            case "attack4":
                return 1;
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