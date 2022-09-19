package tourGuide.tracker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TrackerRun extends Thread {

    private final Tracker tracker;

    public TrackerRun(Tracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void run() {

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                log.debug("Tracker stopping");
                break;
            }

            long watchTime = tracker.trackAllUsers();
            log.debug("Tracker Time Elapsed: " + watchTime + " ms.");

            try {
                log.debug("Tracker sleeping");
                TimeUnit.SECONDS.sleep(60);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
