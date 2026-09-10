package net.azisaba.simplesit.data;

public class PlayerSettings {

    private boolean rightClickSit = true;

    public PlayerSettings() {
    }

    public PlayerSettings(boolean rightClickSit) {
        this.rightClickSit = rightClickSit;
    }

    public boolean isRightClickSit() {
        return rightClickSit;
    }

    public void setRightClickSit(boolean rightClickSit) {
        this.rightClickSit = rightClickSit;
    }
}
