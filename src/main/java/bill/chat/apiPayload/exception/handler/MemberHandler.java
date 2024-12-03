package bill.chat.apiPayload.exception.handler;

import bill.chat.apiPayload.code.BaseErrorCode;
import bill.chat.apiPayload.exception.GeneralException;

public class MemberHandler extends GeneralException {

    public MemberHandler(BaseErrorCode code) {
        super(code);
    }
}
