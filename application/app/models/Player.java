
package models;


public class Player {
    private int points;
    private int warningsReceived;
    private Boolean disconnected;

    public Player() {
        points=0;
        warningsReceived=0;
        disconnected=false;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public int getWarningsReceived() {
        return warningsReceived;
    }

    public int getPoints() {
        return points;
    }

    public void setWarningsReceived(int warningsReceived) {
        this.warningsReceived = warningsReceived;
    }

    public void setDisconnected(Boolean disconnected) {
        this.disconnected = disconnected;
    }

    public Boolean getDisconnected() {
        return disconnected;
    }
    
}
