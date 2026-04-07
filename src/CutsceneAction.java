public class CutsceneAction {
    public String type;
    public String target;
    public int x;
    public int y;
    public int duration;
    public String text;
    public String flag;
    public String value;

    public CutsceneAction(String type) {
        this.type = type;
    }

    public static CutsceneAction waitFrames(int frames) {
        CutsceneAction a = new CutsceneAction("WAIT");
        a.duration = frames;
        return a;
    }

    public static CutsceneAction dialogue(String text) {
        CutsceneAction a = new CutsceneAction("DIALOGUE");
        a.text = text;
        return a;
    }

    public static CutsceneAction setFlag(String flag) {
        CutsceneAction a = new CutsceneAction("SET_FLAG");
        a.flag = flag;
        return a;
    }

    public static CutsceneAction setObjective(String id, String title, String desc) {
        CutsceneAction a = new CutsceneAction("SET_OBJECTIVE");
        a.target = id;
        a.text = title;
        a.value = desc;
        return a;
    }

    public static CutsceneAction movePlayerTo(int x, int y) {
        CutsceneAction a = new CutsceneAction("MOVE_PLAYER");
        a.x = x;
        a.y = y;
        return a;
    }

    public static CutsceneAction facePlayer(String dir) {
        CutsceneAction a = new CutsceneAction("FACE_PLAYER");
        a.value = dir;
        return a;
    }

    public static CutsceneAction teleportPlayer(int x, int y) {
        CutsceneAction a = new CutsceneAction("TELEPORT_PLAYER");
        a.x = x;
        a.y = y;
        return a;
    }

    public static CutsceneAction endCutscene() {
        return new CutsceneAction("END");
    }
}
