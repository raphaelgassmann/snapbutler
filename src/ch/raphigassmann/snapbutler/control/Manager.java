package ch.raphigassmann.snapbutler.control;

import ch.raphigassmann.snapbutler.view.App;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Created by raphaelgassmann on 12.09.16.
 */
public class Manager implements IManager {

    private static IManager manager;
    boolean connectionState = false;

    static ServiceInstance si = null;
    private Folder rootFolder; //Needs knowledge. Is set in connectViSrv()
    static int howDeep=0;
    static int howDeepAdditional=0;
    static int snapshotOnSystemCounter;
    public static int depth = 0;

    /**
     * Default Constructor
     */
    public  Manager (){
        //Plain Object
    }

    /**
     * Initialize all necessary attributes.<br>
     * Need to call before using this manager.
     */
    public void init(IManager manager) {
        this.manager = manager;
        System.out.println("Manager Init done");
    }

    @Override
    public void connectViSrv(String ViSrv, String username, String password) {
        try {
            double startConnectionTime = System.currentTimeMillis();
            si =  new ServiceInstance(new URL("https://"+ ViSrv +"/sdk"), username, password, true);
            //ServiceInstance si =  new ServiceInstance(new URL("https://192.168.178.105/sdk"), "administrator@vsphere.local", "*SSOpassword33", true);
            double endConnectionTime = System.currentTimeMillis();
            System.out.println("Startup Connection in: " + (endConnectionTime - startConnectionTime) + " Milliseconds");

            //Preparing Event
            String startupTaskText = "Connected in "+ ((endConnectionTime-startConnectionTime)/1000) + " Seconds";
            double startUpProgress = 1;
            App.AddTask(startupTaskText, startUpProgress);

            //Creating Root Folder
            rootFolder = si.getRootFolder();
            String name = rootFolder.getName();
            System.out.println("root Folder is: " + name);

            connectionState = true;

        }catch (RemoteException e) {
            System.out.println("Error while establishing VMWare session:");
            connectionState = false;
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        connectionState = true;
    }


    @Override
    public boolean getManagerConnectionState() {
        return connectionState;
    }

    public void fillArray(int row, int colum, String entry){

    }

    @Override
    public String[][] getSystemSnapshotsScanSystem(Boolean scanForCreator, String taskID) {
        String[][] snapshotArray = new String[999][999]; //TODO: Check if posible with dynamic array?

        System.out.println("=========== Snapshots on System ===========");
        ManagedEntity[] mes = new ManagedEntity[0];
        try {
            mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if(mes==null || mes.length ==0)
        {
            Arrays.fill(snapshotArray, null);
            return snapshotArray;
            //return snapshotArray=null;
        }

        //Task Update Variables
        double amountToWork = mes.length;
        double amountToWorkPercentOfVm = 1/amountToWork;

        //snapshotArray = null;
        int vmCounter = 0;

        //Loop through all VMs
        for (int i = 0; i < mes.length ; i++) {
            VirtualMachine vm = (VirtualMachine) mes[i];
            VirtualMachineConfigInfo vminfo = vm.getConfig();
            System.out.println("VM-Name: " + vm.getName());
            howDeepAdditional=0;
            //Check if VM is Template
            if(!vminfo.isTemplate()) {
                listSnapshots(snapshotArray, vm, vmCounter, scanForCreator);
                vmCounter++;
            }
            App.UpdateTask(taskID, "Scan in progress", (i+1)*amountToWorkPercentOfVm);
            System.out.println("----------------------------");
        }

        String[][] result = snapshotArray;

        return result;

    }

    static String[][] listSnapshots(String[][] snapshotArray, VirtualMachine vm, int VmId, Boolean scanForCreator) {
        if(vm==null)
        {
            return null;
        }
        //Check if VM has Snapshot
        if(vm.getSnapshot()!=null) {
            VirtualMachineSnapshotInfo snapInfo = vm.getSnapshot();
            VirtualMachineSnapshotTree[] snapTree = snapInfo.getRootSnapshotList();

            printSnapshots(snapshotArray, snapTree, VmId, vm, scanForCreator);
            depth++;
        }

        return snapshotArray;
    }

    static String[][] printSnapshots(String[][] snapshotArray, VirtualMachineSnapshotTree[] snapTree, int VmId, VirtualMachine vm, Boolean scanForCreator)  {
        for (int i = 0; snapTree!=null && i < snapTree.length; i++)
        {
            VirtualMachineSnapshotTree node = snapTree[i];
            String SnapVmID = "" + VmId;
            System.out.println("Snapshot Name : " + node.getName()+" HowDeep:"+howDeep+" HowDeepAdditional:"+howDeepAdditional+" VmId:"+VmId+"Depth:"+depth);

            snapshotArray[depth][0] = SnapVmID;
            snapshotArray[depth][1] = vm.getName();
            snapshotArray[depth][2] = node.getName();
            snapshotArray[depth][3] = node.getDescription();

            // Create readable date
            int year = node.getCreateTime().get(Calendar.YEAR);
            int month = node.getCreateTime().get(Calendar.MONTH);
            int day = node.getCreateTime().get(Calendar.DAY_OF_MONTH);
            int hour = node.getCreateTime().get(Calendar.HOUR);
            int min = node.getCreateTime().get(Calendar.MINUTE);
            String dateTime = ""+year+"/"+month+"/"+day+" "+hour+":"+min+"";

            snapshotArray[depth][4] = dateTime;
            snapshotArray[depth][5] = "to old to verify a username";

            if (scanForCreator.equals(false)) {
                EventManager evtMgr = si.getEventManager();

                System.out.println("\n=== Print the events per filters ===");
                // create a filter spec for querying events
                EventFilterSpec efs = new EventFilterSpec();
                efs.setType(new String[]{"TaskEvent"});

                EventFilterSpecByEntity eFilter = new EventFilterSpecByEntity();
                eFilter.setEntity(vm.getMOR());
                eFilter.setRecursion(EventFilterSpecRecursionOption.children);
                efs.setEntity(eFilter);

                EventFilterSpecByTime tFilter = new EventFilterSpecByTime();

                Calendar beginTime = null;
                Calendar endTime = null;

                try {
                    beginTime = si.currentTime();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                try {
                    endTime = si.currentTime();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                System.out.println("Before correction: from: " + beginTime.get(Calendar.HOUR) + ":" + beginTime.get(Calendar.MINUTE) + " to: " + endTime.get(Calendar.HOUR) + ":" + endTime.get(Calendar.MINUTE));
                try {

                    int diffDay = si.currentTime().get(Calendar.DAY_OF_MONTH) - node.getCreateTime().get(Calendar.DAY_OF_MONTH);
                    System.out.println("Diff DayOfMonth: " + diffDay);
                    beginTime.add(Calendar.DAY_OF_MONTH, -diffDay);
                    endTime.add(Calendar.DAY_OF_MONTH, -diffDay);

                    int diffMonth = si.currentTime().get(Calendar.MONTH) - node.getCreateTime().get(Calendar.MONTH);
                    System.out.println("Diff Month: " + diffMonth);
                    beginTime.add(Calendar.MONTH, -diffMonth);
                    endTime.add(Calendar.MONTH, -diffMonth);

                    int diffYear = si.currentTime().get(Calendar.YEAR) - node.getCreateTime().get(Calendar.YEAR);
                    System.out.println("Diff Year: " + diffYear);
                    beginTime.add(Calendar.YEAR, -diffYear);
                    endTime.add(Calendar.YEAR, -diffYear);

                    int diffHour = si.currentTime().get(Calendar.HOUR_OF_DAY) - node.getCreateTime().get(Calendar.HOUR_OF_DAY);
                    System.out.println("Diff Hour: " + diffHour);
                    System.out.println("beginTime Hour before: " + beginTime.get(Calendar.HOUR_OF_DAY));
                    beginTime.add(Calendar.HOUR_OF_DAY, -diffHour);
                    beginTime.add(Calendar.HOUR_OF_DAY, -2);
                    System.out.println("beginTime Hour after: " + beginTime.get(Calendar.HOUR_OF_DAY));
                    endTime.add(Calendar.HOUR_OF_DAY, -diffHour);
                    endTime.add(Calendar.HOUR_OF_DAY, -2);


                    int diffMinute = si.currentTime().get(Calendar.MINUTE) - node.getCreateTime().get(Calendar.MINUTE);
                    System.out.println("Diff Minute: " + diffMinute);
                    System.out.println("beginTime Minute before: " + beginTime.get(Calendar.MINUTE));
                    beginTime.add(Calendar.MINUTE, -diffMinute);
                    System.out.println("beginTime Minute after: " + beginTime.get(Calendar.MINUTE));
                    endTime.add(Calendar.MINUTE, -diffMinute);

                    int diffSecond = si.currentTime().get(Calendar.SECOND) - node.getCreateTime().get(Calendar.SECOND);
                    System.out.println("Diff Second: " + diffSecond);
                    beginTime.add(Calendar.SECOND, -diffSecond);
                    endTime.add(Calendar.SECOND, -diffSecond);

                    beginTime.set(Calendar.AM_PM, Calendar.PM);
                    endTime.set(Calendar.AM_PM, Calendar.PM);

                    System.out.println("BeginTime: " + beginTime.getTime());
                    System.out.println("EndTime: " + endTime.getTime());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                //Define tolerance of event filter by time
                if (beginTime != null) {
                    beginTime.add(Calendar.MINUTE, -3);
                }
                if (endTime != null) {
                    endTime.add(Calendar.MINUTE, 3);
                }

                System.out.println("After correction: from: " + beginTime.getTime() + " to: " + endTime.getTime());
                tFilter.setBeginTime(beginTime);
                tFilter.setEndTime(endTime);
                efs.setTime(tFilter);

                Event[] events = new Event[0];
                try {
                    events = evtMgr.queryEvents(efs);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                if (events == null) {
                    System.out.println("Filter by time does not work");
                }

                for (int iEvent = 0; events != null && iEvent < events.length; iEvent++) {
                    if (events[iEvent].getFullFormattedMessage().equals("Task: Create virtual machine snapshot")) {
                        //if(events[iEvent].getFullFormattedMessage() == "Task: Create virtual machine snapshot"){
                        System.out.println(events[iEvent].getUserName());
                        System.out.println("Found Snapshot !!!");
                        //System.out.println("User:" + events[i].getUserName());
                        //System.out.println("VM:" + events[i].getVm().getName());
                        System.out.println("Time:" + events[i].getCreatedTime().getTime());
                        System.out.println("TimeHour:" + events[i].getCreatedTime().get(Calendar.HOUR));
                        System.out.println("TimeMinute:" + events[i].getCreatedTime().get(Calendar.MINUTE));
                        System.out.println("FormattedMessage:" + events[iEvent].getFullFormattedMessage());
                        System.out.println("=================================\n");
                        snapshotArray[depth][5] = events[iEvent].getUserName();
                        //usernameCreator = events[i].getUserName();

                    } else {
                        snapshotArray[depth][5] = "no event to verify";
                    }
                }
            }else{
                snapshotArray[depth][5] = "not scanned";
            }
            VirtualMachineSnapshotTree[] childTree = node.getChildSnapshotList();
            if(childTree!=null)
            {
                depth++;
                howDeep++;
                VmId++;
                printSnapshots(snapshotArray, childTree, VmId, vm, scanForCreator);
            }else{
                howDeepAdditional = howDeepAdditional + howDeep;
                howDeep=0;
            }
        }

        return snapshotArray;
    }

    @Override
    public String[][] validateVMs(String vmToValidate, String taskID) {
        String[][] validatedVms = new String[999][999]; //TODO: Check if possible with dynamic array?
        //Splitter
        String[] vmToCheck = null;
        int SemicolonIndexVmToValidate = vmToValidate.indexOf(';');
        int CommaIndexVmToValidate = vmToValidate.indexOf(',');
        if (SemicolonIndexVmToValidate>=0){
            vmToCheck = vmToValidate.split(";", -1);
        }
        if (CommaIndexVmToValidate>=0){
            vmToCheck = vmToValidate.split(",", -1);
        }
        double amountToWork;
        if (SemicolonIndexVmToValidate == -1 && CommaIndexVmToValidate == -1){
            vmToCheck = vmToValidate.split("^$", -1);
            amountToWork = 1;
        }else{
            amountToWork = vmToCheck.length;
        }
        double amountToWorkPercentOfVm = 1/amountToWork;

        for (int i = 0; i < amountToWork; i++) {
            snapshotOnSystemCounter = 0;
            VirtualMachine vm = null;
            vmToCheck[i] = vmToCheck[i].trim(); //Assign trimmed String again to a string, because Strings are immutable.

            if (vmToCheck[i].isEmpty()){ break;} //Check if String is empty or tab, whitespace between semicolons, or semicolon is at tail

            try {
                vm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmToCheck[i]);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            if(vm==null){
                System.out.println("No VM " + vmToCheck[i] + " found");
            }else{
                VirtualMachineRuntimeInfo vmri = (VirtualMachineRuntimeInfo) vm.getRuntime();

                VirtualMachineSnapshotInfo snapInfo = vm.getSnapshot();
                if(snapInfo == null){
                    snapshotOnSystemCounter = 0;
                }else{
                    VirtualMachineSnapshotTree[] snapTree = snapInfo.getRootSnapshotList();
                    snapshotOnSystemCounter = 0;
                    int counter = 0;
                    countSnapshotsOnSystem(snapTree, counter);
                }
                System.out.println("VM found: " + vmToCheck[i] + " VM power state: " + vmri.getPowerState() + " Snapshots: " + snapshotOnSystemCounter);

                String powerState = ""+vmri.getPowerState()+"";
                String snapshotCounterArray = ""+snapshotOnSystemCounter+"";

                validatedVms[i][0]= vmToCheck[i];
                validatedVms[i][1]= powerState;
                validatedVms[i][2]= snapshotCounterArray;
            }
            App.UpdateTask(taskID, "Validation in progress", (i+1)*amountToWorkPercentOfVm);
        }
        String[][] result = validatedVms;
        return result;
    }

    static int countSnapshotsOnSystem(VirtualMachineSnapshotTree[] snapTree, int numberOfSnapshots){
        for (int i = 0; snapTree!=null && i< snapTree.length; i++) {
            VirtualMachineSnapshotTree node = snapTree[i];
            VirtualMachineSnapshotTree[] childTree = node.getChildSnapshotList();
            snapshotOnSystemCounter++;
            if (childTree!=null){
                countSnapshotsOnSystem(childTree, snapshotOnSystemCounter);
            }

        }
        numberOfSnapshots=snapshotOnSystemCounter;
        return numberOfSnapshots;
    }


    @Override
    public void createSnapshots(Set<ValidatedVm> validatedVMs, String snapshotName, String description, Boolean memory, Boolean quiesce, String taskOption, int groupSize, String taskID) {
        Task task = null;
        int groupCounter = 0;
        int intWorkersAmount = 0;

        for(ValidatedVm s:validatedVMs){
            if(s.getVmName()!= null){intWorkersAmount++;}
        }
        double workersAmount=(int) intWorkersAmount;
        double workersFactor = 1/workersAmount;
        System.out.println("workersFactor: " + workersFactor);
        double workerProgress;
        int workerCounter = 0;
        String taskInfo = "Creating snapshots:";

        for(ValidatedVm s:validatedVMs){
            if(s.getVmName()!=null){
                if(groupCounter == groupSize && taskOption == "Take Snapshots in Group"){
                    try {
                        if (task.waitForTask() == Task.SUCCESS) {
                            //placeholder
                            System.out.println("seen here");
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    groupCounter = 0;
                }
                System.out.println("Starting create Snapshot "+snapshotName+" on VM: "+s.getVmName());
                workerCounter++;
                workerProgress= workerCounter*workersFactor;
                System.out.println("workersProgress: "+workerProgress);
                taskInfo = taskInfo +"\n"+snapshotName+" on "+ s.getVmName();
                App.UpdateTask(taskID,taskInfo,workerProgress);

                try {
                    VirtualMachine vm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", s.getVmName());
                    if(vm!=null)
                    {
                        task = vm.createSnapshot_Task(snapshotName, description, memory, quiesce);
                        if(taskOption == "Take Snapshots sequentially"){
                            try {
                                if(task.waitForTask()==Task.SUCCESS){
                                    //placeholder
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Initiated create Snapshot of VM: " + vm.getName());
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                groupCounter++;
            }
        }
    }

    @Override
    public void deleteSnapshots(Set<Snapshot> selection, String taskOption, int groupSize, String taskID) {
        Task task = null;
        int groupCounter = 0;

        double workersAmount = (double) selection.size();
        double workersFactor = 1/workersAmount;
        System.out.println("workersFactor: "+workersFactor);
        double workerProgress;
        int workerCounter = 0;
        String taskInfo = "Deleting selected snapshots:";

        for(Snapshot s:selection){
            if(groupCounter == groupSize && taskOption == "Delete marked Snapshots in Groups") {
                try {
                    if (task.waitForTask() == Task.SUCCESS) {
                        //placeholder
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                groupCounter = 0;
            }
            System.out.println("Deleting Snapshot: " + s.getSnapshotName());

            workerCounter++;
            workerProgress= workerCounter*workersFactor;
            System.out.println("workersProgress: "+workerProgress);
            taskInfo = taskInfo +"\n"+s.getSnapshotName()+" on "+ s.getVmName();
            App.UpdateTask(taskID,taskInfo,workerProgress);

            try {
                VirtualMachine vm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", s.getVmName());
                VirtualMachineSnapshotTree[] snapshotTree = vm.getSnapshot().getRootSnapshotList();
                VirtualMachineSnapshot vmSnap = getSnapshotInTree(vm, s.getSnapshotName());
                if(vmSnap!=null)
                {
                    task = vmSnap.removeSnapshot_Task(false);
                    if(taskOption == "Delete marked Snapshots sequential"){
                        try {
                            if(task.waitForTask()==Task.SUCCESS){
                                //placeholder
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("Removed snapshot:" + vm.getSnapshot());
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            groupCounter++;
        }



    }

    static VirtualMachineSnapshot getSnapshotInTree(VirtualMachine vm, String snapName){
        if (vm == null || snapName == null){
            return null;
        }

        VirtualMachineSnapshotTree[] snapTree = vm.getSnapshot().getRootSnapshotList();
        if(snapTree!=null){
            ManagedObjectReference mor = findSnapshotInTree(snapTree, snapName);
            if(mor!=null){
                return new VirtualMachineSnapshot(vm.getServerConnection(), mor);
            }
        }
        return null;
    }

    static ManagedObjectReference findSnapshotInTree(VirtualMachineSnapshotTree[] snapTree, String snapName){
        for(int i=0; i <snapTree.length; i++){
            VirtualMachineSnapshotTree node = snapTree[i];
            if(snapName.equals(node.getName())){
                return node.getSnapshot();
            }else{
                VirtualMachineSnapshotTree[] childTree = node.getChildSnapshotList();
                if(childTree!=null){
                    ManagedObjectReference mor = findSnapshotInTree(childTree, snapName);
                    if(mor!=null){
                        return mor;
                    }
                }
            }
        }
        return null;
    }
}

