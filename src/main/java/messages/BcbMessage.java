package messages;

import java.util.UUID;

import pt.unl.fct.di.novasys.babel2.channels.BabelIPMessage;
import pt.unl.fct.di.novasys.babel2.channels.BabelMessage;

@BabelIPMessage(id = 4)
public record BcbMessage(UUID id, BabelMessage message) implements BabelMessage {
}
