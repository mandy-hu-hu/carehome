
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Logger {
    private final List<String> entries = new ArrayList<>();

    public void log(String staffId, String action, String details){
        String line = LocalDateTime.now()+" | staff="+staffId+" | "+action+" | "+details;
        entries.add(line);
        System.out.println(line);
    }

    public void addEntry(String entry) {
        entries.add(entry);
    }

    public void clear() {
        entries.clear();
    }


    public List<String> getEntries(){ return Collections.unmodifiableList(entries); }
}
