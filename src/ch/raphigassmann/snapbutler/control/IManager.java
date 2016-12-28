package ch.raphigassmann.snapbutler.control;

import java.util.Calendar;
import java.util.Set;

/**
 * Created by raphaelgassmann on 12.09.16.
 */
public interface IManager {

    /**
     * Connect to ViServer
     * @param ViSrv
     * @param username
     * @param password
     */
    public void connectViSrv (String ViSrv, String username, String password);

    /**
     * Get Manager Connection State to VIserver
     * @return
     */
    public boolean getManagerConnectionState ();

    /**
     * get all Snaphots on System
     * @return A list of string arrays.<br>
     * [0] = Description [1] = Suffix
     */
    public String[][] getSystemSnapshotsScanSystem(Boolean scanForCreator, String taskID);

    /**
     * Validate VMnames
     * @param vmToValidate
     * @param taskID
     * @return
     */
    public String[][] validateVMs(String vmToValidate, String taskID);

    /**
     * create Snapshot of validated VM Set
     * @param validatedVms
     * @param snapshotName
     * @param description
     * @param memory
     * @param quiesce
     * @param taskOption
     * @param groupSize
     * @param taskID
     */
    public void createSnapshots(Set<ValidatedVm> validatedVms, String snapshotName, String description, Boolean memory, Boolean quiesce, String taskOption, int groupSize, String taskID);

    /**
     * delete a Set of Snapshots
     * @param snapshots
     * @param deleteOption
     * @param groupSize
     * @param taskID
     */
    public void deleteSnapshots(Set<Snapshot> snapshots, String deleteOption, int groupSize, String taskID);

}