package protocols;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import messages.BcbEchoMessage;
import messages.BcbMessage;
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

/**
 * Byzantine Consistent Broadcast — quadratic echo protocol (Bracha 1987).
 *
 * Properties guaranteed:
 *   BCB1 (Validity):     if correct i broadcasts m, every correct process delivers m.
 *   BCB2 (No Dup):       no message delivered more than once.
 *   BCB3 (No Creation):  delivered m sent by correct p was previously broadcast by p.
 *   BCB4 (Consistency):  two correct processes never deliver different values for the same id.
 *
 * Assumption: n >= 3f + 1  (with n=4, f=1).
 * Delivery threshold: count(same-content echos for mid) >= (n + f) / 2
 *                     ↔  2 * count >= n + f  (avoids floating-point).
 */
public class ByzantineConsistentBroadcastProtocol
        implements BroadcastProviderBabelProtocol, MembershipConsumerBabelProtocol, SignedMessageBabelProtocol {

    private static final Logger LOGGER = LogManager.getLogger(ByzantineConsistentBroadcastProtocol.class);

    private final int n; // |π| — total processes (including self)
    private final int f; // max faulty processes

    // echos[mid] maps each process that echoed to the content it echoed
    private final Map<UUID, Map<BabelNode, BabelMessage>> echos;
    private final Set<UUID> delivered;
    // broadcaster[mid] = node that originally sent the MSG for mid
    private final Map<UUID, BabelNode> broadcaster;
    private final Set<BabelNode> neighbors;

    private Babel runtime;
    private BabelChannel channel;

    public ByzantineConsistentBroadcastProtocol(int n, int f) {
        this.n = n;
        this.f = f;
        this.echos = HashMap.newHashMap(16);
        this.delivered = Collections.newSetFromMap(HashMap.newHashMap(16));
        this.broadcaster = HashMap.newHashMap(16);
        this.neighbors = Collections.newSetFromMap(HashMap.newHashMap(n));
    }

    @Override
    public void init(Babel runtime, ComposedBabelChannel channel) {
        this.runtime = runtime;
        this.channel = channel;
    }

    // ── Broadcast request from the application layer ──────────────────────────

    @Override
    public void uponBroadcastRequest(BabelMessage message) {
        UUID mid = UUID.randomUUID();
        LOGGER.info("BCB broadcast request id={}", mid);
        var msg = new SignedBabelMessage<>(new BcbMessage(mid, message));
        for (BabelNode neighbor : neighbors) {
            channel.send(neighbor, msg);
        }
        // Sender also participates in the echo round for its own message
        echo(mid, message);
    }

    // ── Upon receiving initial MSG from broadcaster p ─────────────────────────

    @UponSignedMessageIn
    public void uponBcbMessage(SignedBabelMessage<BcbMessage> signed, BabelNode sender) {
        UUID mid = signed.innerMessage().id();
        BabelMessage m = signed.innerMessage().message();
        LOGGER.info("BCB MSG from {} id={}", sender, mid);
        broadcaster.putIfAbsent(mid, sender);
        echo(mid, m);
    }

    // ── Upon receiving ECHO from process p ────────────────────────────────────

    @UponSignedMessageIn
    public void uponBcbEchoMessage(SignedBabelMessage<BcbEchoMessage> signed, BabelNode sender) {
        UUID mid = signed.innerMessage().id();
        BabelMessage m = signed.innerMessage().message();
        LOGGER.info("BCB ECHO from {} id={}", sender, mid);

        Map<BabelNode, BabelMessage> msgEchos = echos.computeIfAbsent(mid, k -> new HashMap<>());
        msgEchos.putIfAbsent(sender, m);

        if (!delivered.contains(mid)) {
            long count = msgEchos.values().stream().filter(m::equals).count();
            // Deliver when: count >= (n + f) / 2  ↔  2*count >= n+f
            if (2 * count >= n + f) {
                delivered.add(mid);
                BabelNode original = broadcaster.getOrDefault(mid, sender);
                LOGGER.info("BCB deliver id={} original={} count={}/{}", mid, original, count, n);
                this.deliverMessage(runtime, m, original);
            }
        }
    }

    // ── Shared echo logic ─────────────────────────────────────────────────────

    private void echo(UUID mid, BabelMessage m) {
        Map<BabelNode, BabelMessage> msgEchos = echos.computeIfAbsent(mid, k -> new HashMap<>());
        // Only echo once per message id
        if (msgEchos.putIfAbsent(runtime.getMyself(), m) == null) {
            var echoMsg = new SignedBabelMessage<>(new BcbEchoMessage(mid, m));
            for (BabelNode neighbor : neighbors) {
                channel.send(neighbor, echoMsg);
            }
        }
    }

    // ── Membership callbacks ──────────────────────────────────────────────────

    @Override
    public void neighbourUp(BabelNode node) {
        LOGGER.info("BCB neighbor up {}", node);
        neighbors.add(node);
    }

    @Override
    public void neigbourDown(BabelNode node) {
        LOGGER.info("BCB neighbor down {}", node);
        neighbors.remove(node);
    }
}
