import java.awt.image.BufferedImage;

public class BattlePlayer {
    public Entity player;
    public double x, y;
    public double targetX, targetY;
    public int hp, mp;
    public int maxHp, maxMp;
    public BufferedImage image;
    public PlayerAnimation anim;

    public void playAnimation(String name) {
        if (anim != null) {
            boolean loop =
                    name.equals("idle") ||
                    name.equals("lowHpIdle") ||
                    name.equals("run_left") ||
                    name.equals("run_right");

            anim.setAnimation(name, loop);
        }
    }

    public BufferedImage getCurrentImage() {
        if (anim != null) {
            return anim.getCurrentImage();
        }
        return image;
    }

    public void update() {
        if (anim != null) {
            anim.update();
        }
    }

    public boolean isAnimationFinished() {
        return anim == null || anim.isFinished();
    }

    public boolean isOnStrikeFrame() {
        return anim != null && anim.isOnStrikeFrame();
    }
}
