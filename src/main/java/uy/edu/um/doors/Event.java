package uy.edu.um.doors;

import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.MyList;

public class Event {

    public enum EventType {

        CPU, RAM, DISK

    }

    private EventType type;
    private MyList<String> instructions;

    public Event(EventType type) {
        this.type = type;
        this.instructions = new MyLinkedListImpl<>();

    }

    public void addInstruction(String instruction) {
        this.instructions.add(instruction);

    }

    public EventType getType() {
        return this.type;
    }

    public MyList<String> getInstructions() {
        return this.instructions;
    }

    @Override
    public String toString() {
        return "Event: " + type + "| Instructions: " + instructionsToString();
    }

    private String instructionsToString() {
        String result = "[";
        for(int i = 0; i < instructions.size(); i++) {
            if(i > 0) result += ", ";
            result += instructions.get(i);
        }
        return result += "]";
    }


}
