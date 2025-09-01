package model;

public class SubTask extends Task {
    private int epicId;

    @Override
    public String toString() {
        return "model.SubTask{" +
                "epicId=" + epicId +
                "} " + super.toString();
    }

    public SubTask() {
        super(); // по умолчанию
    }
    public SubTask(int id, String name, String description,int epicId) {
        super(id, name, description);
        this.epicId = epicId;
    }

    public int getEpicId() {
        return epicId;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    }
}
