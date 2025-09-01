package model;

import java.util.List;

public class Epic extends Task {
    private List<Integer> subTaskIds;

    @Override
    public String toString() {
        return "model.Epic{" +
                "subTaskIds=" + subTaskIds +
                "} " + super.toString();
    }

    public Epic() {
        super(); // по умолчанию
    }

    public Epic(int id, String name, String description) {
        super(id, name, description);
    }

    public List<Integer> getSubTaskIds() {
        return subTaskIds;
    }

    public void setSubTaskIds(List<Integer> subTaskIds) {
        this.subTaskIds = subTaskIds;
    }
}
