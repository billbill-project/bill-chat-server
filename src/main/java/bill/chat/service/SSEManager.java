package bill.chat.service;

import bill.chat.dto.SSEDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@Component
@Slf4j
public class SSEManager {

    private final Map<String, Sinks.Many<SSEDTO>> sinks = new ConcurrentHashMap<>();

    public Sinks.Many<SSEDTO> getOrManageSink(String userId) {
        return sinks.computeIfAbsent(userId, id -> {
            log.info("getOrManageSink 들어옴");
            Sinks.Many<SSEDTO> sink = Sinks.many()
                    .unicast()
                    .onBackpressureBuffer();

            sink.asFlux()
                    .doFinally(signalType -> sinks.remove(id));

            return sink;
        });
    }

    public boolean doesSinkExist(String userId) {
        Many<SSEDTO> foundSink = sinks.get(userId);
        return foundSink != null && foundSink.currentSubscriberCount() > 0;
    }
}
