package tourGuide.model.nearby;

import gpsUtil.location.Attraction;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NbAttraction extends Attraction implements Comparable<NbAttraction> {
    private double distance;
    private int rewardPoints;


    public NbAttraction(String attractionName, String city, String state, double latitude, double longitude, double distance, int rewardPoints) {
        super(attractionName, city, state, latitude, longitude);
        this.distance = distance;
        this.rewardPoints = rewardPoints;
    }

    @Override
    public int compareTo(NbAttraction nbAttraction) {
        return (int)(this.distance - nbAttraction.getDistance());
}
}
