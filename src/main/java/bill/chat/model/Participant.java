package bill.chat.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Participant {
    private String userId;
    private String role; //owner, contact
    private boolean notification;

    public void changeNotification() {
        if (notification) {
            notification = false;
            return;
        }
        notification = true;
    }
}
