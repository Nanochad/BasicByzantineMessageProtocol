package protocols;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import pt.unl.fct.di.novasys.babel2.channels.BabelChannel;
import pt.unl.fct.di.novasys.babel2.channels.ComposedBabelChannel;
import pt.unl.fct.di.novasys.babel2.core.Babel;
import pt.unl.fct.di.novasys.babel2.core.BabelNode;
import pt.unl.fct.di.novasys.babel2.core.membership.MembershipProviderBabelProtocol;

public class StaticMembershipProtocol implements MembershipProviderBabelProtocol {
    private static final Logger LOGGER = LogManager.getLogger(StaticMembershipProtocol.class);

    private final List<BabelNode> neighbors;

    private Babel runtime;
    private BabelChannel channel;

    public StaticMembershipProtocol(List<BabelNode> neighbors) {
        this.neighbors = List.copyOf(neighbors);
    }

    @Override
    public void init(Babel runtime, ComposedBabelChannel channel) {
        this.runtime = runtime;
        this.channel = channel;
        var it = neighbors.iterator();
        var node = it.next();
        while (!node.equals(runtime.getMyself())) {
            channel.connect(node);
            node = it.next();
        }
    }

    @Override
    public void connectionDown(BabelNode peer, @Nullable Throwable cause) {
        LOGGER.info("Host {} down", peer);
        this.neighbourDown(runtime, peer);
    }

    @Override
    public void connectionFailed(BabelNode peer, @Nullable Throwable cause) {
        LOGGER.info("Node {} failed... retrying", peer);
        this.neighbourDown(runtime, peer);
        channel.connect(peer);
    }

    @Override
    public void connectionUp(BabelNode peer) {
        LOGGER.info("Node {} is up", peer);
        this.neighbourUp(runtime, peer);
    }
}
