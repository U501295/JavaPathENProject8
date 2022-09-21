package tourGuide;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.model.nearby.RecommandedAttractions;
import tourGuide.model.user.UserMostRecentLocation;
import tourGuide.model.user.UserPreferences;
import tourGuide.service.TourGuideService;
import tourGuide.model.user.User;
import tripPricer.Provider;

@Slf4j
@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public String getLocation(@RequestParam String userName) {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		return JsonStream.serialize(visitedLocation.location);
    }
    
    @RequestMapping("/getNearbyAttractions")
    public RecommandedAttractions getNearbyAttractions(@RequestParam String userName) {
    	return tourGuideService.getRecommandedAttractions(userName);
    }
    
    @RequestMapping("/getRewards") 
    public String getRewards(@RequestParam String userName) {
    	return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }
    
    @RequestMapping("/getAllCurrentLocations")
    public List<UserMostRecentLocation> getAllCurrentLocations() {
    	// TODO: Get a list of every user's most recent location as JSON
    	//- Note: does not use gpsUtil to query for their current location, 
    	//        but rather gathers the user's current location from their stored location history.
    	//
    	// Return object should be the just a JSON mapping of userId to Locations similar to:
    	//     {
    	//        "019b04a9-067a-4c76-8817-ee75088c3822": {"longitude":-48.188821,"latitude":74.84371} 
    	//        ...
    	//     }

        return tourGuideService.getAllCurrentLocations();
    }
    
    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
    	List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
    	return JsonStream.serialize(providers);
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }

    @PutMapping("/userPreferences/{userName}")
    public ResponseEntity<?> putUserPreferences(@PathVariable String userName,
                                                @RequestBody UserPreferences userPreferences) {

        log.debug("request for set userPreferences of userName : {}", userName);

        try {
            User userSaved = tourGuideService.updateUserPreferences(userName, userPreferences);
            return ResponseEntity.status(HttpStatus.OK).body(userSaved);
        } catch (NoSuchElementException e) {
            String logAndBodyMessage = "error while putting user because missing user with userName=" + userName;
            log.error(logAndBodyMessage);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(logAndBodyMessage);
        }
    }
   

}