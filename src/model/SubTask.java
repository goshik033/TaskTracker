package model;

import java.time.Duration;
import java.time.LocalDateTime;

public class SubTask extends Task {
    private int epicId;

    @Override
    public String toString() {
        return "SubTask{" +
                "epicId=" + epicId +", "
                + super.toString().substring(5);
    }

    public SubTask() {
        super(); // по умолчанию
    }
    public SubTask(int id, String name, String description, Status status, LocalDateTime startTime, Duration duration, int epicId) {
        super(id, name, description,status, startTime, duration);
        this.epicId = epicId;
    }

    public int getEpicId() {
        return epicId;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    }
}
