package tourGuide.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.springframework.stereotype.Service;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.model.user.User;
import tourGuide.model.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;

	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	private ExecutorService calculateRewardsThreadPool = Executors.newFixedThreadPool(100);
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void calculateRewardsAwaitTerminationAfterShutdown() {
		calculateRewardsThreadPool.shutdown();
		try {
			if (!calculateRewardsThreadPool.awaitTermination(5, TimeUnit.MINUTES)) {
				calculateRewardsThreadPool.shutdownNow();
			}
		} catch (InterruptedException ex) {
			calculateRewardsThreadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
		calculateRewardsThreadPool = Executors.newFixedThreadPool(100);
	}

	public Future<Void> calculateRewards(User user) {
		return CompletableFuture.supplyAsync(() -> {
			List<VisitedLocation> userLocations = user.getVisitedLocations();
			List<Attraction> attractions = gpsUtil.getAttractions();
			for (VisitedLocation visitedLocation : userLocations) {
				attractions.parallelStream().forEach(attraction -> {
					if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))){
						boolean isNear = nearAttraction(visitedLocation, attraction);
						if (isNear) {
							user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction.attractionId, user)));
						}
					}
				});
			}
			return null;
		}, calculateRewardsThreadPool);
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		int attractionProximityRange = 200;
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}


	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(UUID attractionId, User user) {
		return rewardsCentral.getAttractionRewardPoints(attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
