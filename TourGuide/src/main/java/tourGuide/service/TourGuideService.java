package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.model.nearby.NbAttraction;
import tourGuide.model.nearby.NbUser;
import tourGuide.model.nearby.RecommandedAttractions;
import tourGuide.model.user.UserPreferences;
import tourGuide.tracker.Tracker;
import tourGuide.model.user.User;
import tourGuide.model.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
@Slf4j
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	private ExecutorService trackUserLocationThreadPool = Executors.newFixedThreadPool(100);
	
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		trackUserLocationAwaitTerminationAfterShutdown();
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}
	
	public VisitedLocation getUserLocation(User user) {
		try {
			return (user.getVisitedLocations().size() > 0) ?
					user.getLastVisitedLocation() :
					trackUserLocation(user).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey,
				user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(),
				user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(),
				cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}
	
	public Future<VisitedLocation> trackUserLocation(User user) {
		return CompletableFuture.supplyAsync(() -> {
			VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
			user.addToVisitedLocations(visitedLocation);
			try {
				rewardsService.calculateRewards(user).get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("trackUserLocation failed");
				throw new RuntimeException(e);
			}
			return visitedLocation;
		}, trackUserLocationThreadPool);
	}

	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		List<Attraction> nearbyAttractions = new ArrayList<>();
		for(Attraction attraction : gpsUtil.getAttractions()) {
			if(rewardsService.isWithinAttractionProximity(attraction, visitedLocation.location)) {
				nearbyAttractions.add(attraction);
			}
		}
		
		return nearbyAttractions;
	}

	public List<NbAttraction> getFiveNearestAttractions(User user, Location location) {

		return gpsUtil.getAttractions().stream()
				.map(attraction -> {
							NbAttraction nbAttraction = new NbAttraction(attraction.attractionName,
									attraction.city, attraction.state, attraction.latitude, attraction.longitude
									, rewardsService.getDistance(location, new Location(attraction.latitude, attraction.longitude))
									,0);
							return nbAttraction;
						}
				).sorted()
				.limit(5)
				.parallel()
				.peek(nbAttraction -> nbAttraction.setRewardPoints(rewardsService.getRewardPoints(nbAttraction.attractionId, user)))
				.collect(Collectors.toList());
	}

	public RecommandedAttractions getRecommandedAttractions(String userName) {
		RecommandedAttractions recommandedAttractions = new RecommandedAttractions();
		User user = getUser(userName);
		Location userLocation = getUserLocation(user).location;

		recommandedAttractions.setNbUser(new NbUser(userLocation));

		recommandedAttractions.setRecoAttractions(getFiveNearestAttractions(user, userLocation));

		return recommandedAttractions;
	}

	public void trackUserLocationAwaitTerminationAfterShutdown() {
		trackUserLocationThreadPool.shutdown();
		try {
			if (!trackUserLocationThreadPool.awaitTermination(5, TimeUnit.MINUTES)) {
				trackUserLocationThreadPool.shutdownNow();
			}
		} catch (InterruptedException ex) {
			trackUserLocationThreadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
		trackUserLocationThreadPool = Executors.newFixedThreadPool(100);
	}

	public User updateUserPreferences(String userName, UserPreferences userPreferences) {
		User user = getUser(userName);

		if (null == user) {
			throw new NoSuchElementException();
		}
		user.setUserPreferences(userPreferences);
		return internalUserMap.replace(user.getUserName(), user);
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
	
}
