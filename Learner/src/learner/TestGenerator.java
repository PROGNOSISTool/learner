package learner;

import java.io.IOException;
import java.util.List;

public interface TestGenerator {
    public void initialize();
    public List<String> nextTest() throws IOException;
    public void terminate();
}
