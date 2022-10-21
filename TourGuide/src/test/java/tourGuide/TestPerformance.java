package tourGuide;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.model.user.User;
import tourGuide.tracker.Tracker;

@Slf4j
public class TestPerformance {
	
	/*
	 * A note on performance improvements:
	 *     
	 *     The number of users generated for the high volume tests can be easily adjusted via this method:
	 *     
	 *     		InternalTestHelper.setInternalUserNumber(100 000);
	 *     
	 *     
	 *     These tests can be modified to suit new solutions, just as long as the performance metrics
	 *     at the end of the tests remains consistent. 
	 * 
	 *     These are performance metrics that we are trying to hit:
	 *     
	 *     highVolumeTrackLocation: 100,000 users within 15 minutes:
	 *     		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     *     highVolumeGetRewards: 100,000 users within 20 minutes:
	 *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */

	@BeforeClass
	public static void setUpAllTests() {
		Locale.setDefault(Locale.US);
	}

	@Test
	public void highVolumeTrackLocation() {
		InternalTestHelper.setInternalUserNumber(100);
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		Tracker tracker = new Tracker(tourGuideService);

		long watchTime = tracker.trackAllUsers();

		log.info("highVolumeTrackLocation: Time Elapsed: " + watchTime + " ms");
		assertTrue(TimeUnit.MINUTES.toMillis(15) >= watchTime);
	}

	@Test
	public void highVolumeCalculateRewards() {
		InternalTestHelper.setInternalUserNumber(100);
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		StopWatch stopWatch = new StopWatch();
		Attraction fakeAttraction = new Attraction("attractionName", "city", "state", 0, 0);
		Date fakeDate = new Date();

		List<User> allUsers = tourGuideService.getAllUsers();
		allUsers.parallelStream()
				.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), fakeAttraction, fakeDate)));

		stopWatch.start();
		allUsers.forEach(rewardsService::calculateRewards);
		rewardsService.calculateRewardsAwaitTerminationAfterShutdown();
		stopWatch.stop();
		long watchTime = stopWatch.getTime();

		log.info("highVolumeCalculateRewards: Time Elapsed: " + watchTime + " ms");
		assertTrue(TimeUnit.MINUTES.toMillis(20) >= watchTime);
	}
	
}
