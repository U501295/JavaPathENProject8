package tourGuide.model.nearby;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecommandedAttractions {
    private NbUser nbUser;
    private List<NbAttraction> recoAttractions;
}
