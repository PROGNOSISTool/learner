package learner;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PredefinedTestGenerator implements TestGenerator {
    private List<LinkedList<String>> testSuite;
    
    public PredefinedTestGenerator(List<LinkedList<String>> testSuite) {
        this.testSuite = testSuite;
    } 

    @Override
    public void initialize() {
        // TODO Auto-generated method stub

    }

    @Override
    public List<String> nextTest() throws IOException {
        if(testSuite.isEmpty())
            return null;
        else 
            return testSuite.remove(0);
    }

    @Override
    public void terminate() {
        testSuite.clear();
    }

}
