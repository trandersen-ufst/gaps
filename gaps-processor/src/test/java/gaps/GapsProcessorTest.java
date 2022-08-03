package gaps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GapsProcessorTest {
    @Test
    @DisplayName("silence never used warning")
    public void silenceNeverUsedWarning() {
        new GapsProcessor();
    }
}