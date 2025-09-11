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
        super();
    }

    public Epic(int id, String name, String description,Status status) {
        super(id, name, description,status );

    }

    public List<Integer> getSubTaskIds() {
        return subTaskIds;
    }

    public void setSubTaskIds(List<Integer> subTaskIds) {
        this.subTaskIds = subTaskIds;
    }
}
