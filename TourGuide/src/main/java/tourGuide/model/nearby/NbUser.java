package tourGuide.model.nearby;

import gpsUtil.location.Location;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NbUser {
    private Location location;

    public NbUser(Location location) {
        this.location = location;
    }
}
