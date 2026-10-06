package clubflow.event;

import java.time.LocalDate;

public class Event {
    private String tag;
    private String name;
    private LocalDate date;
    private LocalDate deadline;
    private double budget;
    private String venue;
    private String logistics;

    public Event(String tag, String name, LocalDate date, LocalDate deadline,
                 double budget, String venue, String logistics) {
        this.tag = tag;
        this.name = name;
        this.date = date;
        this.deadline = deadline;
        this.budget = budget;
        this.venue = venue;
        this.logistics = logistics;
    }

    public String getTag() {
        return tag;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public double getBudget() {
        return budget;
    }

    public String getVenue() {
        return venue;
    }

    public String getLogistics() {
        return logistics;
    }
}