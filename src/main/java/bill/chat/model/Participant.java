package bill.chat.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Participant {

    private String userId;
    private String role; //seller or buyer
    private boolean isLeft;

    public void updateLeft() {
        isLeft = true;
    }
}
