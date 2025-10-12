
import java.time.*;
import java.util.*;

public class Logger {
    private final List<String> entries = new ArrayList<>();

    public void log(String staffId, String action, String details){
        String line = LocalDateTime.now()+" | staff="+staffId+" | "+action+" | "+details;
        entries.add(line);
        System.out.println(line);
    }

    public List<String> getEntries(){ return Collections.unmodifiableList(entries); }
}
