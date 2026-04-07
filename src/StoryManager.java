import java.io.*;
import java.util.HashSet;

public class StoryManager {
    private HashSet<String> flags = new HashSet<>();

    public String currentChapter = "monk_ch1";
    public StoryObjective currentObjective =
            new StoryObjective("escape_monastery", "Escape the Monastery",
                    "Find a way out before the attackers reach the inner hall.");

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public void setFlag(String flag) {
        flags.add(flag);
    }

    public void clearFlag(String flag) {
        flags.remove(flag);
    }

    public void setObjective(String id, String title, String description) {
        currentObjective = new StoryObjective(id, title, description);
    }

    public void save() {
        try {
            File saveDir = new File("res/save");
            if (!saveDir.exists()) saveDir.mkdirs();

            PrintWriter writer = new PrintWriter(new FileWriter("res/save/story_flags.txt"));
            writer.println("chapter=" + currentChapter);

            if (currentObjective != null) {
                writer.println("objective_id=" + safe(currentObjective.id));
                writer.println("objective_title=" + safe(currentObjective.title));
                writer.println("objective_desc=" + safe(currentObjective.description));
            }

            for (String flag : flags) {
                writer.println("flag=" + flag);
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        File file = new File("res/save/story_flags.txt");
        if (!file.exists()) return;

        flags.clear();

        String objId = "";
        String objTitle = "";
        String objDesc = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("chapter=")) {
                    currentChapter = line.substring("chapter=".length()).trim();
                } else if (line.startsWith("objective_id=")) {
                    objId = uns(line.substring("objective_id=".length()).trim());
                } else if (line.startsWith("objective_title=")) {
                    objTitle = uns(line.substring("objective_title=".length()).trim());
                } else if (line.startsWith("objective_desc=")) {
                    objDesc = uns(line.substring("objective_desc=".length()).trim());
                } else if (line.startsWith("flag=")) {
                    flags.add(line.substring("flag=".length()).trim());
                }
            }

            br.close();

            if (!objId.isEmpty() || !objTitle.isEmpty() || !objDesc.isEmpty()) {
                currentObjective = new StoryObjective(objId, objTitle, objDesc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\n", "\\n");
    }

    private String uns(String s) {
        return s.replace("\\n", "\n");
    }
}
