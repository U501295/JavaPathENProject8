package tourGuide.model.user;

import gpsUtil.location.VisitedLocation;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserMostRecentLocation {
    private UUID userId;
    private VisitedLocation mostRecentLocation;
}
