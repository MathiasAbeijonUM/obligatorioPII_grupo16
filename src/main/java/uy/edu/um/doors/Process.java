package uy.edu.um.doors;

import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.MyList;

public class Process {

    public enum ProcessState {

        NEW, PENDING, RUNNING, FINISHED

    }

    private int pid;
    private String name;
    private User user;
    private int priority;
    private ProcessState state;
    private MyList<Event> events;

    public Process(int pid, String name, User user) {
        this.pid = pid;
        this.name = name;
        this.user = user;
        this.priority = 0;
        this.state = ProcessState.NEW;
        this.events = new MyLinkedListImpl<>();
    }

    public void addEvent(Event event) {
        this.events.add(event);

    }

    public int getPid() {
        return this.pid;
    }

    public String getName() {
        return this.name;
    }

    public User getUser() {
        return this.user;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public ProcessState getState() {
        return this.state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public MyList<Event> getEvents() {
        return this.events;
    }

    @Override
    public String toString() {
        return "PID=" + pid + " " + name + " " + user.toString() + " " + priority;
    }

}
