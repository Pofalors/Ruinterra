public class Quest {
    public String name;
    public String description;
    public boolean completed = false;
    public boolean active = false;
    public String id = "";
    public int stage = 0;
    
    // Στόχοι quest
    public String targetItem; // Αν θέλει να μαζέψεις κάτι
    public int requiredAmount;
    public int currentAmount;
    
    // Αν θέλει να μιλήσεις σε κάποιον
    public String targetNPC;
    public boolean talkedToNPC = false;
    
    // Ανταμοιβή
    public Item rewardItem;
    public int rewardGold;
    
    public Quest(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public boolean checkCompletion() {
        if (targetItem != null && currentAmount >= requiredAmount) {
            completed = true;
            return true;
        }
        if (targetNPC != null && talkedToNPC) {
            completed = true;
            return true;
        }
        return false;
    }
}