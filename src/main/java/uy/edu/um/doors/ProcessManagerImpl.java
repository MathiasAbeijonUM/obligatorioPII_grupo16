package uy.edu.um.doors;

import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
import uy.edu.um.tad.stack.MyStack;
import uy.edu.um.tad.stack.MyStackImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ProcessManagerImpl implements ProcessManager{

    //EL DISEÑO DE LA ESTRUCTURA DE ALMACENAMIENTO DEBE IMPLEMENTARSE EN ESTA CLASE EN RELACIÓN CON LAS ENTIDADES QUE DEFINA

    // Procesos que fueron cargados, pero todavía no fueron preparados.
    private MyQueue<Process> newProcesses;

    // Procesos ya preparados. Después se usa para ejecutar el de mayor prioridad.
    private MyHeap<Process> pendingProcesses;

    // Único proceso que puede estar ejecutándose, porque Doors es monotarea.
    private Process runningProcess;

    // Últimos procesos finalizados que siguen cargados en memoria.
    private MyStack<Process> finishedProcesses;

    // Datos cargados desde los CSV.
    private MyList<User> users;
    private MyList<Process> allProcesses;

    public ProcessManagerImpl() {
        reset();
    }

    @Override
    public void loadProcessAndUserData(String processCsv, String usersCsv) {
        reset(); // Limpia la memoria antes de cargar datos nuevos

        try {
            loadUsers(usersCsv);
            loadProcesses(processCsv); // Falta definir este metodo auxiliar


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

            users.add(user);
        }

        reader.close();
    }

    @Override
    public void prepareProcesses() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void executeNextProcess() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessOk() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessError() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void terminateProcess(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatus() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByUser(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByProcess(int pid) {
        System.out.println("IMPLEMENTAR");
    }

    private void reset() {
        this.newProcesses = new MyQueueImpl<>();
        this.pendingProcesses = new MyHeapImpl<>(false);
        this.runningProcess = null;
        this.finishedProcesses = new MyStackImpl<>();
        this.users = new MyLinkedListImpl<>();
        this.allProcesses = new MyLinkedListImpl<>();
    }
}
