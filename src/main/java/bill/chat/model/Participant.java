package bill.chat.model;

import java.time.OffsetDateTime;

public class Participant {
    private final OffsetDateTime createdAt = OffsetDateTime.now();

    private String userId;
    private String role; //seller or buyer
    private boolean isLeft;
}
