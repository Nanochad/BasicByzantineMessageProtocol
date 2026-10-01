import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import protocols.AutomatedDisseminationAppFactory;
import protocols.ByzantineConsistentBroadcastProtocol;
import protocols.StaticMembershipProtocol;
import pt.unl.fct.di.novasys.babel2.channels.DefaultNodeStore;
import pt.unl.fct.di.novasys.babel2.channels.PublicKeyBabelNode;
import pt.unl.fct.di.novasys.babel2.channels.tcp_tls.AbstractTCPBabelChannel.TCPBabelChannelAuthMode;
import pt.unl.fct.di.novasys.babel2.channels.tcp_tls.TCPBabelChannel;
import pt.unl.fct.di.novasys.babel2.channels.udp.UDPBabelChannel;
import pt.unl.fct.di.novasys.babel2.channels.utils.BabelIpUtils;
import pt.unl.fct.di.novasys.babel2.core.Babel;
import pt.unl.fct.di.novasys.babel2.core.BabelNode;
import pt.unl.fct.di.novasys.babel2.core.NettyExecutorProtocolHolderFactory;
import pt.unl.fct.di.novasys.babel2.core.broadcast.BroadcastConsumerBabelProtocol;
import pt.unl.fct.di.novasys.babel2.core.broadcast.BroadcastConsumerExtensionFactory;
import pt.unl.fct.di.novasys.babel2.core.broadcast.BroadcastProviderBabelProtocol;
import pt.unl.fct.di.novasys.babel2.core.broadcast.BroadcastProviderExtensionFactory;
import pt.unl.fct.di.novasys.babel2.core.membership.MembershipConsumerBabelProtocol;
import pt.unl.fct.di.novasys.babel2.core.membership.MembershipConsumerExtensionFactory;
import pt.unl.fct.di.novasys.babel2.core.membership.MembershipProviderBabelProtocol;
import pt.unl.fct.di.novasys.babel2.core.membership.MembershipProviderExtensionFactory;
import pt.unl.fct.di.novasys.babel2.core.resolver.ResolverProvideExtensionFactory;
import pt.unl.fct.di.novasys.babel2.core.resolver.ResolverProviderBabelProtocol;
import pt.unl.fct.di.novasys.babel2.core.signed.SignedMessageBabelProtocol;
import pt.unl.fct.di.novasys.babel2.core.signed.SignedMessageExtensionFactory;
import pt.unl.fct.di.novasys.babel2.protocols.discovery.LocalNetworkDiscoveryProtocol;
import pt.unl.fct.di.novasys.babel2.utils.BabelPropertiesReader;
import utils.BabelIPComparator;
import utils.Crypto;

public class Main {

    private static final String DEFAULT_CONFIG_FILE = "babel.conf";

    public static void main(String[] args) throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException {

        var properties = BabelPropertiesReader.loadConfig(Paths.get(DEFAULT_CONFIG_FILE), args);

        var eventLoop = BabelIpUtils.createLoopGroup(5);
        var nettyHolderFactory = new NettyExecutorProtocolHolderFactory(4096, eventLoop);

        var nodeStore = new DefaultNodeStore(4, BabelIPComparator.INSTANCE);

        var trustStore = Crypto.getTruststore(properties);
        var keyPair = Crypto.getKeyPair(properties);

        List<BabelNode> membership = new ArrayList<>(4);
        var it = trustStore.aliases();
        while (it.hasMoreElements()) {
            membership.add(new PublicKeyBabelNode(trustStore.getCertificate(it.nextElement()).getPublicKey()));
        }

        var tcpBuilder = TCPBabelChannel.builder()
                .setEventLoopGroup(eventLoop)
                .setPermittedNodes(Set.copyOf(membership))
                .verifyIdentity()
                .setAuthMode(TCPBabelChannelAuthMode.FULL_AUTH)
                .setNumberOfConnections(3)
                .setNumberOfMessages(2);

        var udpBuilder = UDPBabelChannel.builder()
                .setEventLoopGroup(eventLoop);

        var resolver = new LocalNetworkDiscoveryProtocol(nodeStore);
        var disseminationApp = new AutomatedDisseminationAppFactory().createInstance(properties);
        var broadcastProtocol = new ByzantineConsistentBroadcastProtocol(membership.size(), 1);
        var membershipProtocol = new StaticMembershipProtocol(membership);

        Babel.builder(3, 4, 1, 4, 2)
                .setNodeStore(nodeStore)
                .registerExtension(MembershipConsumerBabelProtocol.class, new MembershipConsumerExtensionFactory())
                .registerExtension(MembershipProviderBabelProtocol.class, new MembershipProviderExtensionFactory())
                .registerExtension(BroadcastConsumerBabelProtocol.class, new BroadcastConsumerExtensionFactory())
                .registerExtension(BroadcastProviderBabelProtocol.class, new BroadcastProviderExtensionFactory())
                .registerExtension(ResolverProviderBabelProtocol.class, new ResolverProvideExtensionFactory())
                .registerExtension(SignedMessageBabelProtocol.class, new SignedMessageExtensionFactory())
                .registerProtocol(resolver, nettyHolderFactory, udpBuilder, tcpBuilder)
                .registerProtocol(disseminationApp, nettyHolderFactory, tcpBuilder)
                .registerProtocol(broadcastProtocol, nettyHolderFactory, tcpBuilder)
                .registerProtocol(membershipProtocol, nettyHolderFactory, tcpBuilder)
                .setKeyPair(keyPair)
                .build()
                .init();
    }
}
