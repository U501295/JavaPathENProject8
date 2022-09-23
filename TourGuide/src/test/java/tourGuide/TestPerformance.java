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


	@Ignore
	@Test
	public void highVolumeTrackLocation() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		// Users should be incremented up to 100,000, and test finishes within 15 minutes
		InternalTestHelper.setInternalUserNumber(100);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		List<User> allUsers = new ArrayList<>();
		allUsers = tourGuideService.getAllUsers();
		
	    StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		List<VisitedLocation> cmpt = new ArrayList<>();
		for(User user : allUsers) {
			try {
				cmpt.add(tourGuideService.trackUserLocation(user).get());
			} catch (InterruptedException | ExecutionException e) {
				log.error("trackUserLocation failed");
				throw new RuntimeException(e);
			}
		}
		stopWatch.stop();
		tourGuideService.trackUserLocationAwaitTerminationAfterShutdown();

		System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds."); 
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}


	@Ignore
	@Test
	public void highVolumeGetRewards() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		// Users should be incremented up to 100,000, and test finishes within 20 minutes
		InternalTestHelper.setInternalUserNumber(100);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
	    Attraction attraction = gpsUtil.getAttractions().get(0);
		List<User> allUsers = new ArrayList<>();
		allUsers = tourGuideService.getAllUsers();
		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

		allUsers.forEach(u -> {
			try {
				rewardsService.calculateRewards(u).get();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		});


		for(User user : allUsers) {
			int size = user.getUserRewards().size();
			assertTrue(user.getUserRewards().size() > 0);
		}
		stopWatch.stop();
		tourGuideService.trackUserLocationAwaitTerminationAfterShutdown();

		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds."); 
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

	@Test
	public void highVolumeTrackLocationF() {
		InternalTestHelper.setInternalUserNumber(1000);
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		Tracker tracker = new Tracker(tourGuideService);

		long watchTime = tracker.trackAllUsers();

		log.info("highVolumeTrackLocation: Time Elapsed: " + watchTime + " ms");
		assertTrue(TimeUnit.MINUTES.toMillis(15) >= watchTime);
	}

	@Test
	public void highVolumeCalculateRewardsF() {
		InternalTestHelper.setInternalUserNumber(10000);
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
