import java.util.ArrayList;

public class CutscenePlayer {
    private GamePanel gp;
    private ArrayList<CutsceneAction> actions = new ArrayList<>();
    private int index = 0;
    private int waitTimer = 0;
    private boolean running = false;
    private boolean waitingDialogueClose = false;

    public CutscenePlayer(GamePanel gp) {
        this.gp = gp;
    }

    public void start(ArrayList<CutsceneAction> newActions) {
        actions.clear();
        actions.addAll(newActions);
        index = 0;
        waitTimer = 0;
        running = true;
        waitingDialogueClose = false;
        gp.gameState = gp.cutsceneState;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
        actions.clear();
        index = 0;
        waitTimer = 0;
        waitingDialogueClose = false;
        gp.gameState = gp.playState;
    }

    public void update() {
        if (!running) return;

        if (index >= actions.size()) {
            stop();
            return;
        }

        if (waitingDialogueClose) {
            if (gp.gameState != gp.dialogueState) {
                waitingDialogueClose = false;
                gp.gameState = gp.cutsceneState;
                index++;
            }
            return;
        }

        CutsceneAction action = actions.get(index);

        switch (action.type) {
            case "WAIT":
                waitTimer++;
                if (waitTimer >= action.duration) {
                    waitTimer = 0;
                    index++;
                }
                break;

            case "DIALOGUE":
                gp.startDialogue(action.text);
                gp.gameState = gp.dialogueState;
                waitingDialogueClose = true;
                break;

            case "SET_FLAG":
                gp.storyManager.setFlag(action.flag);
                index++;
                break;

            case "SET_OBJECTIVE":
                gp.storyManager.setObjective(action.target, action.text, action.value);
                gp.showObjectivePopup(action.text, action.value);
                index++;
                break;

            case "MOVE_PLAYER":
                movePlayerToward(action.x, action.y);
                if (Math.abs(gp.player.worldX - action.x) <= gp.player.speed &&
                    Math.abs(gp.player.worldY - action.y) <= gp.player.speed) {
                    gp.player.worldX = action.x;
                    gp.player.worldY = action.y;
                    index++;
                }
                break;

            case "UNLOCK_PARTY_MEMBER":
                gp.unlockPartyMember(action.value);
                index++;
                break;

            case "START_BATTLE":
                gp.startStoryBattle(action.value);
                index++;
                break;

            case "FACE_PLAYER":
                gp.player.direction = action.value;
                gp.player.updateImage();
                index++;
                break;

            case "TELEPORT_PLAYER":
                gp.player.worldX = action.x;
                gp.player.worldY = action.y;
                index++;
                break;

            case "END":
                stop();
                break;

            default:
                index++;
                break;
        }
    }

    private void movePlayerToward(int targetX, int targetY) {
        if (gp.player.worldX < targetX) {
            gp.player.worldX += gp.player.speed;
            gp.player.direction = "right";
        } else if (gp.player.worldX > targetX) {
            gp.player.worldX -= gp.player.speed;
            gp.player.direction = "left";
        }

        if (gp.player.worldY < targetY) {
            gp.player.worldY += gp.player.speed;
            gp.player.direction = "down";
        } else if (gp.player.worldY > targetY) {
            gp.player.worldY -= gp.player.speed;
            gp.player.direction = "up";
        }

        gp.player.updateImage();
    }
}
