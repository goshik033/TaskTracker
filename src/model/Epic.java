package model;

import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private List<Integer> subTaskIds = new ArrayList<Integer>();

    @Override
    public String toString() {
        return "Epic{" +
                "subTaskIds=" + subTaskIds + ", "
                + super.toString().substring(5);
    }

    public Epic() {
        super(); // по умолчанию
    }

    public Epic(int id, String name, String description,Status status, List<Integer> subTaskIds) {
        super(id, name, description,status );
        this.subTaskIds = subTaskIds;
    }

    public List<Integer> getSubTaskIds() {
        return subTaskIds;
    }

    public void setSubTaskIds(List<Integer> subTaskIds) {
        this.subTaskIds = subTaskIds;
    }
}
