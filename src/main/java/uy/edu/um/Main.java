package uy.edu.um;

import uy.edu.um.doors.ProcessManager;
import uy.edu.um.doors.ProcessManagerImpl;

public class Main {
    public static void main(String[] args) {

        ProcessManager pm = new ProcessManagerImpl();

        pm.loadProcessAndUserData("process.csv", "users.csv");
        pm.prepareProcesses();
        pm.executeNextProcess();

        pm.printStatusByProcess(19520);
    }
}