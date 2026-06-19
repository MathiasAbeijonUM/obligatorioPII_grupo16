package uy.edu.um.doors;

import uy.edu.um.tad.heap.EmptyHeapException;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.queue.EmptyQueueException;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
import uy.edu.um.tad.stack.MyStack;
import uy.edu.um.tad.stack.MyStackImpl;
import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProcessManagerImpl implements ProcessManager {

    // EL DISEÑO DE LA ESTRUCTURA DE ALMACENAMIENTO DEBE IMPLEMENTARSE EN ESTA CLASE EN RELACIÓN CON LAS ENTIDADES QUE DEFINA

    // Procesos que fueron cargados, pero todavía no fueron preparados.
    private MyQueue<Process> newProcesses;

    // Procesos ya preparados. Después se usa para ejecutar el de mayor prioridad.
    private MyHeap<Process> pendingProcesses;

    // Único proceso que puede estar ejecutándose, porque Doors es monotarea.
    private Process runningProcess;

    // Últimos procesos finalizados que siguen cargados en memoria.
    private MyStackImpl<Process> finishedProcesses;

    // Datos cargados desde los CSV.
    private MyHash<Integer, User> users;
    private MyList<Process> allProcesses;

    public ProcessManagerImpl() {
        reset();
    }

    @Override
    public void loadProcessAndUserData(String processCsv, String usersCsv) {
        reset(); // Limpia la memoria antes de cargar datos nuevos

        try {
            loadUsers(usersCsv);
            loadProcesses(processCsv); 

        } catch (IOException e) {
            System.out.println("Error leyendo archivos CSV: " + e.getMessage());
            reset(); // Limpia la memoria por si se cargaron datos antes de que ocurriera el error

        } catch (RuntimeException e) {
            System.out.println("Error cargando datos: " + e.getMessage());
            reset();
        }
    }

    private void loadUsers(String usersCsv) throws IOException {
        // Abre el archivo para leerlo
        BufferedReader reader = new BufferedReader(new FileReader(usersCsv));

        String line = reader.readLine(); // saltea encabezado

        // Lee el resto de las lineas del archivo
        while ((line = reader.readLine()) != null) {

            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split(";");

            int uid = Integer.parseInt(parts[0].trim());
            String alias = parts[1].trim();
            User.UserType type = User.UserType.valueOf(parts[2].trim().toUpperCase());

            User user = new User(uid, alias, type);

            users.put(uid, user);
        }

        reader.close();
    }






    private void loadProcesses(String processCsv) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(processCsv));

        String line = reader.readLine(); // saltea encabezado

        while ((line = reader.readLine()) != null) {

            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split(";", 4);

            int pid = Integer.parseInt(parts[0].trim());
            int uid = Integer.parseInt(parts[1].trim());
            String name = parts[2].trim();
            String eventsRaw = parts[3].trim();

            User user = findUserByUid(uid);

            if (user == null) {
                throw new RuntimeException("No existe usuario con UID: " + uid);
            }

            Process process = new Process(pid, name, user);

            loadEventsIntoProcess(process, eventsRaw);

            newProcesses.enqueue(process);
            allProcesses.add(process);
        }

        reader.close();
    }

    private void loadEventsIntoProcess(Process process, String eventsRaw) {
        eventsRaw = eventsRaw.trim();

        if (eventsRaw.startsWith("{")) {
            eventsRaw = eventsRaw.substring(1);
        }

        if (eventsRaw.endsWith("}")) {
            eventsRaw = eventsRaw.substring(0, eventsRaw.length() - 1);
        }

        String[] events = eventsRaw.split("#");

        for (String rawEvent : events) {
            Event event = parseEvent(rawEvent.trim());
            process.addEvent(event);
        }
    }

    private Event parseEvent(String rawEvent) {
        String[] parts = rawEvent.split(":", 2);

        Event.EventType type = Event.EventType.valueOf(parts[0].trim().toUpperCase());

        Event event = new Event(type);

        String instructionsRaw = parts[1].trim();

        if (instructionsRaw.startsWith("[")) {
            instructionsRaw = instructionsRaw.substring(1);
        }

        if (instructionsRaw.endsWith("]")) {
            instructionsRaw = instructionsRaw.substring(0, instructionsRaw.length() - 1);
        }

        String[] instructions = instructionsRaw.split(",");

        for (String instruction : instructions) {
            String cleanInstruction = instruction.trim();

            if (!cleanInstruction.isEmpty()) {
                event.addInstruction(cleanInstruction);
            }
        }

        return event;
    }

    private User findUserByUid(int uid) {
        return users.get(uid);
    }






    @Override
    public void prepareProcesses() {
        while (!newProcesses.isEmpty()) {
            try {
                Process process = newProcesses.dequeue();

                int priority = calculatePriority(process);
                process.setPriority(priority);
                process.setState(Process.ProcessState.PENDING);

                pendingProcesses.insert(process);

                logNewPendingProcess(process);

            } catch (EmptyQueueException e) {
                System.out.println("Error preparando procesos nuevos");
            }
        }
    }

    private int calculatePriority(Process process) {
        int cpuCount = 0;
        int ramCount = 0;
        int diskCount = 0;

        for (int i = 0; i < process.getEvents().size(); i++) {
            Event event = process.getEvents().get(i);

            if (event.getType() == Event.EventType.CPU) {
                cpuCount++;
            } else if (event.getType() == Event.EventType.RAM) {
                ramCount++;
            } else if (event.getType() == Event.EventType.DISK) {
                diskCount++;
            }
        }

        int totalEvents = process.getEvents().size();

        if (totalEvents == 0) {
            return 0;
        }

        int userWeight = getUserWeight(process.getUser());

        return ((8 * cpuCount + 2 * ramCount + 2 * diskCount) / totalEvents)
                + userWeight * totalEvents;
    }

    private int getUserWeight(User user) {
        if (user.getType() == User.UserType.ADMIN) {
            return 32;
        }

        return 16;
    }

    private void logNewPendingProcess(Process process) {
        writeToLogFile(
                "[" + getCurrentTimestamp() + "]: NEW PENDING PROCESS: PID=" + process.getPid()
                        + " | " + process.getName()
                        + " | USER:" + process.getUser().getAlias()
                        + " UID:" + process.getUser().getUid()
                        + " | P=" + process.getPriority()
        );
    }






    @Override
    public void executeNextProcess() {
        if (runningProcess != null) {
            System.out.println("Ya hay un proceso en ejecución: PID=" + runningProcess.getPid());
            return;
        }

        if (pendingProcesses.isEmpty()) {
            System.out.println("No hay procesos pendientes para ejecutar");
            return;
        }

        try {
            Process process = pendingProcesses.remove();

            process.setState(Process.ProcessState.RUNNING);
            runningProcess = process;

            logExecutingProcess(process);

        } catch (EmptyHeapException e) {
            System.out.println("No hay procesos pendientes para ejecutar");
        }
    }

    private void logExecutingProcess(Process process) {
        writeToLogFile(
                "[" + getCurrentTimestamp() + "]: EXECUTING PROCESS: PID=" + process.getPid()
                        + " | USER:" + process.getUser().getAlias()
                        + " UID:" + process.getUser().getUid()
        );

        for (int i = 0; i < process.getEvents().size(); i++) {
            Event event = process.getEvents().get(i);

            writeToLogFile(
                    "EVENT: " + event.getType()
                            + " | Instructions " + instructionsToString(event)
            );
        }
    }





    @Override
    public void finishProcessOk() {
        if (runningProcess == null) {
            System.out.println("No hay proceso en ejecución para finalizar");
            return;
        }

        runningProcess.setState(Process.ProcessState.FINISHED);
        runningProcess.setFinishState(Process.FinishState.OK);

        logEndingProcessOk(runningProcess);

        pushFinishedProcessBasic(runningProcess);

        runningProcess = null;
    }


    private void logEndingProcessOk(Process process) {
        writeToLogFile(
                "[" + getCurrentTimestamp() + "]: ENDING PROCESS: PID=" + process.getPid()
                        + " | STATE: OK"
        );
    }

    private void pushFinishedProcessBasic(Process process) {
        if (finishedProcesses.size() == MAX_FINISHED_PROCESS_ON_RAM) {
            writeToLogFile("Finished process stack overflow");

            while (!finishedProcesses.isEmpty()) {
                try {
                    Process finished = finishedProcesses.pop();

                    writeToLogFile(
                            "PID=" + finished.getPid()
                                    + " " + finished.getName()
                                    + " | STATE: " + finished.getFinishState()
                                    + " | USER:" + finished.getUser().getAlias()
                                    + " UID:" + finished.getUser().getUid()
                    );

                } catch (Exception e) {
                    System.out.println("Error vaciando pila de finalizados");
                }
            }
        }

        finishedProcesses.push(process);
    }

    private String instructionsToString(Event event) {
        String result = "[";

        for (int i = 0; i < event.getInstructions().size(); i++) {
            if (i > 0) {
                result += ", ";
            }

            result += event.getInstructions().get(i);
        }

        result += "]";

        return result;
    }

    private String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    private void writeToLogFile(String message) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String fileName = "DOORS_PROCESS_LOG_" + date;

        try {
            java.io.FileWriter writer = new java.io.FileWriter(fileName, true);
            writer.write(message + System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            System.out.println("Error escribiendo en el log: " + e.getMessage());
        }
    }

    @Override
    public void finishProcessError() {
        if (runningProcess == null) {
            System.out.println("No hay proceso en ejecución para finalizar");
            return;
        }

        runningProcess.setState(Process.ProcessState.FINISHED);
        runningProcess.setFinishState(Process.FinishState.ERROR);

        writeToLogFile(
                "[" + getCurrentTimestamp() + "]: ENDING PROCESS: PID=" + runningProcess.getPid()
                        + " | STATE: ERROR"
        );

        pushFinishedProcessBasic(runningProcess);

        runningProcess = null;
    }

    @Override
    public void terminateProcess(int uid) {
        if (runningProcess == null) {
            System.out.println("No hay proceso en ejecución para finalizar");
            return;
        }

        User terminatingUser = findUserByUid(uid);

        if (terminatingUser == null) {
            System.out.println("No existe usuario con UID: " + uid);
            return;
        }

        runningProcess.setState(Process.ProcessState.FINISHED);
        runningProcess.setFinishState(Process.FinishState.TERMINATED);

        writeToLogFile(
                "[" + getCurrentTimestamp() + "]: ENDING PROCESS: PID=" + runningProcess.getPid()
                        + " | STATE: TERMINATED by USER:" + terminatingUser.getAlias()
                        + " UID:" + terminatingUser.getUid()
        );

        pushFinishedProcessBasic(runningProcess);

        runningProcess = null;
    }

    @Override
    public void printStatus() {
        System.out.println("PROCESS STATUS");

        System.out.println("EXECUTING:");
        if (runningProcess != null) {
            System.out.println("\t" + runningProcess.toString());
        }

        System.out.println("PENDING:");
        for (int i = 0; i < allProcesses.size(); i++) {
            Process p = allProcesses.get(i);
            if (p.getState() == Process.ProcessState.PENDING) {
                System.out.println("\t" + p.toString());
            }
        }

        System.out.println("FINISHED:");
        for (int i = finishedProcesses.size() - 1; i >= 0; i--) {
            Process p = finishedProcesses.get(i);
            System.out.println("\tPID=" + p.getPid() + " " + p.getName()
                    + " | STATE: " + p.getFinishState()
                    + " | USER:" + p.getUser().getAlias()
                    + " UID:" + p.getUser().getUid());
        }
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("PROCESS STATUS VERBOSE");

        System.out.println("EXECUTING:");
        if (runningProcess != null) {
            printProcessVerbose(runningProcess);
        }

        System.out.println("PENDING:");
        for (int i = 0; i < allProcesses.size(); i++) {
            Process process = allProcesses.get(i);

            if (process.getState() == Process.ProcessState.PENDING) {
                printProcessVerbose(process);
            }
        }

        System.out.println("FINISHED:");
        for (int i = finishedProcesses.size() - 1; i >= 0; i--) {
            Process process = finishedProcesses.get(i);
            printProcessVerbose(process);
        }
    }

    @Override
    public void printStatusByUser(int uid) {
        User user = findUserByUid(uid);

        if (user == null) {
            System.out.println("No existe usuario con UID: " + uid);
            return;
        }

        System.out.println("PROCESS STATUS BY USER");
        System.out.println("USER:" + user.getAlias() + " UID:" + user.getUid());

        boolean found = false;

        for (int i = 0; i < allProcesses.size(); i++) {
            Process process = allProcesses.get(i);

            if (process.getUser().getUid() == uid && isProcessLoadedInMemory(process)) {
                printProcessBasic(process);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No hay procesos cargados en memoria para el usuario indicado");
        }
    }

    @Override
    public void printStatusByProcess(int pid) {
        Process process = findProcessByPid(pid);

        if (process == null || !isProcessLoadedInMemory(process)) {
            System.out.println("No existe proceso cargado en memoria con PID: " + pid);
            return;
        }

        System.out.println("PROCESS STATUS BY PID");
        printProcessVerbose(process);
    }

    private Process findProcessByPid(int pid) {
        for (int i = 0; i < allProcesses.size(); i++) {
            Process process = allProcesses.get(i);

            if (process.getPid() == pid) {
                return process;
            }
        }

        return null;
    }

    private boolean isProcessLoadedInMemory(Process process) {
        if (process.getState() == Process.ProcessState.FINISHED) {
            return isFinishedProcessInRam(process);
        }

        return true;
    }

    private boolean isFinishedProcessInRam(Process process) {
        for (int i = 0; i < finishedProcesses.size(); i++) {
            Process finished = finishedProcesses.get(i);

            if (finished.getPid() == process.getPid()) {
                return true;
            }
        }

        return false;
    }

    private void printProcessBasic(Process process) {
        System.out.println("\tPID=" + process.getPid()
                + " " + process.getName()
                + " | STATE: " + process.getState()
                + " | USER:" + process.getUser().getAlias()
                + " UID:" + process.getUser().getUid()
                + " | P=" + process.getPriority());

        if (process.getFinishState() != null) {
            System.out.println("\tFINISH STATE: " + process.getFinishState());
        }
    }

    private void printProcessVerbose(Process process) {
        printProcessBasic(process);

        for (int i = 0; i < process.getEvents().size(); i++) {
            Event event = process.getEvents().get(i);

            System.out.println("\tEVENT: " + event.getType()
                    + " | Instructions " + instructionsToString(event));
        }
    }

    private void reset() {
        this.newProcesses = new MyQueueImpl<>();
        this.pendingProcesses = new MyHeapImpl<>(false);
        this.runningProcess = null;
        this.finishedProcesses = new MyStackImpl<>();
        this.users = new MyHashImpl<>();
        this.allProcesses = new MyLinkedListImpl<>();
    }
}
