package uy.edu.um;

import uy.edu.um.doors.ProcessConsole;
import uy.edu.um.doors.ProcessManager;
import uy.edu.um.doors.ProcessManagerImpl;

public class Main {
    public static void main(String[] args) {

        ProcessManager pm = new ProcessManagerImpl();
        ProcessConsole console = new ProcessConsole(pm);
        console.init();

    }
}