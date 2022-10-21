package tourGuide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.*;
import java.util.concurrent.ExecutionException;

import gpsUtil.location.Location;
import org.junit.BeforeClass;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.model.nearby.NbAttraction;
import tourGuide.model.nearby.RecommandedAttractions;
import tourGuide.model.user.UserMostRecentLocation;
import tourGuide.model.user.UserPreferences;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.model.user.User;
import tripPricer.Provider;

public class TestTourGuideService {

	@BeforeClass
	public static void setUpAllTests() {
		Locale.setDefault(Locale.US);
	}
	@Test
	public void getUserLocation() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		VisitedLocation visitedLocation;
		try {
			visitedLocation = tourGuideService.trackUserLocation(user).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

		assertEquals(user.getUserId(), visitedLocation.userId);
	}
	
	@Test
	public void addUser() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);
		
		User retrivedUser = tourGuideService.getUser(user.getUserName());
		User retrivedUser2 = tourGuideService.getUser(user2.getUserName());

		tourGuideService.trackUserLocationAwaitTerminationAfterShutdown();
		
		assertEquals(user, retrivedUser);
		assertEquals(user2, retrivedUser2);
	}
	
	@Test
	public void getAllUsers() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);
		
		List<User> allUsers = tourGuideService.getAllUsers();

		tourGuideService.trackUserLocationAwaitTerminationAfterShutdown();
		
		assertTrue(allUsers.contains(user));
		assertTrue(allUsers.contains(user2));
	}
	
	@Test
	public void trackUser() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		VisitedLocation visitedLocation;
		try {
			visitedLocation = tourGuideService.trackUserLocation(user).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

		tourGuideService.trackUserLocationAwaitTerminationAfterShutdown();
		
		assertEquals(user.getUserId(), visitedLocation.userId);
	}

	@Test
	public void getRecommandedAttractions() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");


		VisitedLocation visitedLocation;
		try {
			visitedLocation = tourGuideService.trackUserLocation(user).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		tourGuideService.addUser(user);

		RecommandedAttractions recommandedAttractions = tourGuideService.getRecommandedAttractions(user.getUserName());

		tourGuideService.trackUserLocationAwaitTerminationAfterShutdown();

		assertEquals(5, 5);

	}

	@Test
	public void updateUserPreferences() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		User user = new User(UUID.randomUUID(), "updateUserPreferences", "000", "updateUserPreferences@tourGuide.com");
		tourGuideService.addUser(user);
		int beforeUpdate = user.getUserPreferences().getNumberOfChildren();
		tourGuideService.updateUserPreferences(user.getUserName(), new UserPreferences(2,2,2));
		int afterUpdate = user.getUserPreferences().getNumberOfChildren();
		assertNotEquals(beforeUpdate, afterUpdate);
	}



	@Test
	public void get5NearbyAttractions() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		try {
			tourGuideService.trackUserLocation(user).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

		List<NbAttraction> attr = tourGuideService.getFiveNearestAttractions(user,user.getLastVisitedLocation().location);

		tourGuideService.trackUserLocationAwaitTerminationAfterShutdown();
		
		assertEquals(5, attr.size());
	}

	@Test
	public void getTripDeals() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

		List<Provider> providers = tourGuideService.getTripDeals(user);

		assertEquals(5, providers.size());
	}

	@Test
	public void getAllCurrentLocations() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		UUID userId = UUID.randomUUID();
		User user1 = new User(userId, "getAllCurrentLocations", "000", "getAllCurrentLocations@tourGuide.com");
		User user2 = new User(userId, "getAllCurrentLocations2", "0002", "getAllCurrentLocations@tourGuide.com2");
		//user.set(Collections.singletonList(new VisitedLocation(userId, new Location(1, 2), new Date())));
		try {
			tourGuideService.trackUserLocation(user1).get();
			tourGuideService.trackUserLocation(user2).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		tourGuideService.addUser(user1);
		tourGuideService.addUser(user2);
		List<UserMostRecentLocation> usersMostRecentLocations = tourGuideService.getAllCurrentLocations();

		assertNotEquals(null, usersMostRecentLocations.get(0));
		assertEquals(2, usersMostRecentLocations.size());
	}
	
	
}
