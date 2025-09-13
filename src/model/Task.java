package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public  class Task {
    private int id;
    private String name;
    private String description;
    private Status status;
    private LocalDateTime startTime;
    private Duration duration;
    public Task() {
        this.status = Status.NEW; // по умолчанию
    }
    public Task(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = Status.NEW;
    }
    public Task(int id, String name, String description, Status status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id && Objects.equals(name, task.name) && Objects.equals(description, task.description) && status == task.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, status);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                '}';
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    public LocalDateTime getEndTime() {
        return startTime.plus(duration);
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Duration getDuration() {
        return duration;
    }
}