package be.uantwerpen.sc.tools;

/**
 * Created by Arthur on 28/04/2016.
 */
public class DriveDir {

    DriveDirEnum dir;
    double angle = 90;
    DriveDir(){

    }

    public DriveDir(DriveDirEnum dir){
        this.dir = dir;
    }

    @Override
    public String toString(){
        switch(dir){
            case FORWARD:
                return "DRIVE FORWARD 120";
            case LEFT:
                return "DRIVE TURN L " + angle;
            case RIGHT:
                return "DRIVE TURN R " + angle;
            case FOLLOW:
                return "DRIVE FOLLOWLINE";
            case TURN:
                return "DRIVE ROTATE R 180";
            default:
                return "MISSING";
        }
    }

    public DriveDirEnum getDir() {
        return dir;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }
}