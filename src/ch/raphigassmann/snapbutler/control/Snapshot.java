package ch.raphigassmann.snapbutler.control;

/**
 * Created by raphaelgassmann on 30.09.16.
 */
public class Snapshot {
    private String vmName;
    private String snapshotName;
    private String description;
    private String createdTime;
    private String createdBy;


    public Snapshot(String s, String s1, String s2, String s3, String s4, String s5){
        this.vmName = "";
        this.snapshotName = "";
        this.description = "";
        this.createdTime = "";
        this.createdBy = "";
    }

    public Snapshot(String vmName, String snapshotName, String description, String createdTime, String createdBy){
        this.vmName = vmName;
        this.snapshotName = snapshotName;
        this.description = description;
        this.createdTime = createdTime;
        this.createdBy = createdBy;
    }

    public String getVmName(){
        return vmName;
    }

    public void setVmName(String vmName){
        this.vmName = vmName;
    }

    public String getSnapshotName(){
        return snapshotName;
    }

    public void setSnapshotName(String snapshotName){
        this.snapshotName = snapshotName;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public String getCreatedTime(){
        return createdTime;
    }

    public void setCreatedTime(String createdTime){
        this.createdTime = createdTime;
    }

    public String getCreatedBy(){
        return createdBy;
    }

    public void setCreatedBy(String createdBy){
        this.createdBy = createdBy;
    }


}

