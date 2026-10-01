package protocols;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import messages.BroadcastMessage;
import messages.EchoBroadcastMessage;
import pt.unl.fct.di.novasys.babel2.channels.BabelChannel;
import pt.unl.fct.di.novasys.babel2.channels.BabelMessage;
import pt.unl.fct.di.novasys.babel2.channels.ComposedBabelChannel;
import pt.unl.fct.di.novasys.babel2.channels.SignedBabelMessage;
import pt.unl.fct.di.novasys.babel2.core.Babel;
import pt.unl.fct.di.novasys.babel2.core.BabelNode;
import pt.unl.fct.di.novasys.babel2.core.broadcast.BroadcastProviderBabelProtocol;
import pt.unl.fct.di.novasys.babel2.core.membership.MembershipConsumerBabelProtocol;
import pt.unl.fct.di.novasys.babel2.core.signed.SignedMessageBabelProtocol;
import pt.unl.fct.di.novasys.babel2.core.signed.annotations.UponSignedMessageIn;

public class CrashFaultReliableBroadcastProtocol
        implements BroadcastProviderBabelProtocol, MembershipConsumerBabelProtocol, SignedMessageBabelProtocol {
    private static final Logger LOGGER = LogManager.getLogger(CrashFaultReliableBroadcastProtocol.class);
    public static final int DEFAULT_EXPECTED_MESSAGES = 20;
    public static final int DEFAULT_EXPECTED_NEIGHBORS = 4;

    private final Set<UUID> delivered;
    private final Set<BabelNode> neighbors;

    /**
     * Relevant objects to communicate with the Babel 2 framework.
     * The runtime object is for intra machine protocol communication. The channel
     * object is for inter machine communication.
     * The Babel object can only have one implementation. while BabelChannel can
     * have many. Ideally, the protocol code should not know what is the specific
     * BabelChannel implementation being used, only what guarantees it provides.
     */
    private Babel runtime;
    private BabelChannel channel;

    public CrashFaultReliableBroadcastProtocol() {
        this(DEFAULT_EXPECTED_MESSAGES, DEFAULT_EXPECTED_NEIGHBORS);
    }

    public CrashFaultReliableBroadcastProtocol(int expectedMessages, int expectedNeighbors) {
        delivered = Collections.newSetFromMap(HashMap.newHashMap(expectedMessages));
        neighbors = Collections.newSetFromMap(HashMap.newHashMap(expectedNeighbors));
    }

    public void init(Babel runtime, ComposedBabelChannel channel) {
        this.runtime = runtime;
        this.channel = channel;
    }

    /**
     * Redirect it to uponBroadcastMessage
     * 
     * @param message
     * @param sender
     */
    @UponSignedMessageIn
    public void uponEchoBroadcastMessage(SignedBabelMessage<EchoBroadcastMessage> message, BabelNode sender) {
        uponBroadcastMessage(message.innerMessage().message(), sender);
    }

    /**
     * Check if this broadcast message has been seen and retransmit it if it has
     * not.
     * 
     * @param message
     * @param sender
     */
    @UponSignedMessageIn
    public void uponBroadcastMessage(SignedBabelMessage<BroadcastMessage> message, BabelNode sender) {
        if (delivered.add(message.innerMessage().id())) {
            LOGGER.info("New message {} from {}, retransmitting it...", message.innerMessage(), sender);
            // We need to encapsulate the BroadcastMessage in a new SignedBabelMessage so
            // that the channel generates a new signature
            var broadcastMessage = new SignedBabelMessage<>(new EchoBroadcastMessage(message));
            sendToEveryone(broadcastMessage);

            // We unpack the various layers to reach the SuperImportantMessage and its original sender
            this.deliverMessage(runtime, message.innerMessage().message(),
                    message.sender());
        }
    }

    @Override
    public void uponBroadcastRequest(BabelMessage message) {
        LOGGER.info("Broadcast request for {}... sending to current membership", message);
        var broadcastMessage = new SignedBabelMessage<>(
                new BroadcastMessage(UUID.randomUUID(), message));
        sendToEveryone(broadcastMessage);
    }

    @Override
    public void neighbourUp(BabelNode node) {
        LOGGER.info("NEIGHBOR UP: {}", node);
        this.neighbors.add(node);
    }

    @Override
    public void neigbourDown(BabelNode node) {
        LOGGER.info("NEIGBOR DOWN: {}", node);
        this.neighbors.remove(node);
    }

    private void sendToEveryone(BabelMessage message) {
        for (var neighbor : this.neighbors) {
            this.channel.send(neighbor, message);
        }
    }
}
