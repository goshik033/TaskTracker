import java.util.List;

public class Epic extends Task{
    private List<Integer> subtaskIds;

    @Override
    public String toString() {
        return "Epic{" +
                "subtaskIds=" + subtaskIds +
                "} " + super.toString();
    }

    public Epic() {
        super(); // по умолчанию
    }

    public Epic(int id, String name, String description) {
        super(id, name, description);
    }

    public List<Integer> getSubtaskIds() {
        return subtaskIds;
    }

    public void setSubtaskIds(List<Integer> subtaskIds) {
        this.subtaskIds = subtaskIds;
    }
}
