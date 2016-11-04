package ch.teko.gassmanr.control;

/**
 * Created by raphaelgassmann on 30.09.16.
 */
public class ValidatedVm {

    private String vmName;
    private String powerState;
    private String snapshotsOnSystem;

/*
    public ValidatedVm(String v, String v1, String v2){
        this.vmName = "";
        this.powerState = "";
        this.snapshotsOnSystem = "";
    }
*/

    public ValidatedVm(String vmName, String powerState, String snapshotsOnSystem){
        this.vmName = vmName;
        this.powerState = powerState;
        this.snapshotsOnSystem = snapshotsOnSystem;

    }

    public String getVmName(){
        return vmName;
    }

    public void setVmName(String vmName){
        this.vmName = vmName;
    }

    public String getPowerState(){
        return powerState;
    }

    public void setPowerState(String powerState){
        this.powerState = powerState;
    }

    public String getSnapshotsOnSystem(){
        return snapshotsOnSystem;
    }

    public void setSnapshotsOnSystem(String snapshotsOnSystem){
        this.snapshotsOnSystem = snapshotsOnSystem;
    }

}

