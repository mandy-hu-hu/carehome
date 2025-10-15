import java.time.LocalDateTime;

public class Roster {
    private final LocalDateTime start;
    private final LocalDateTime end;

    public Roster(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "[" + start + " - " + end + "]";
    }
}
