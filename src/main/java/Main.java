import java.security.cert.CertificateException;

import org.jspecify.annotations.NullMarked;

import protocols.AutomatedDisseminationAppFactory;
import protocols.CrashFaultReliableBroadcastProtocol;
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

final String DEFAULT_CONFIG_FILE = "babel.conf";

void main(String[] args) throws IOException, KeyStoreException,
        NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException {

    // This reads a properties file like in the original Babel. It is not mandatory
    // to read command line arguments or a config file using this method, unlike the
    // original Babel. This method is left for backwards compatibility sake.
    var properties = BabelPropertiesReader.loadConfig(Paths.get(DEFAULT_CONFIG_FILE), args);

    // The execution model is different compared to the original Babel. Not much
    // control was given in regards to how the framework managed the threads. One
    // thread per protocol, and double the number of cores as threads for the
    // underlying channels/netty (the default). It is now possible to pick from many
    // different execution policies to ensure maximum performance. For this example,
    // we create a netty thread pool of 3 threads shared between the protocols. The
    // protocol handler atomic execution is guaranteed in any executor policy.
    var eventLoop = BabelIpUtils.createLoopGroup(5);
    var nettyHolderFactory = new NettyExecutorProtocolHolderFactory(4096, eventLoop);

    // The NodeStore is where the framework stores information on how to contact
    // each node (IP address + port). Here, we create the DefaultNodeStore
    // implementaion that only keeps information for as long as the program lives.
    // Other implementations can save the information to the disk. In this case we
    // expect 4 nodes total. The comparator is used to set the priority for the
    // addresses found for each node. Each time a protocol tries to connect, it
    // will try to connect in the sequence determined by the comparator.
    var nodeStore = new DefaultNodeStore(4, BabelIPComparator.INSTANCE);

    // We load the trust store and also read all the public keys stored in the trust
    // store. That will also be our membership
    var trustStore = Crypto.getTruststore(properties);
    var keyPair = Crypto.getKeyPair(properties);

    List<BabelNode> membership = new ArrayList<>(4);
    var it = trustStore.aliases();
    while (it.hasMoreElements()) {
        membership.add(new PublicKeyBabelNode(trustStore.getCertificate(it.nextElement()).getPublicKey()));
    }

    // Build the TCP channel. The lack of a binding address will bind this channel
    // to the wildcard address (0.0.0.0 for IPv4 and [::] for IPv6) and some
    // available port in the system.
    var tcpBuilder = TCPBabelChannel.builder()
            .setEventLoopGroup(eventLoop) // Set the event loop we created earlier
            .setPermittedNodes(Set.copyOf(membership)) // We only accept connections from these nodes
            .verifyIdentity() // They must prove their identity with a challenge
            .setAuthMode(TCPBabelChannelAuthMode.FULL_AUTH) // Only signed messages are allowed in this channel
            .setNumberOfConnections(3) // We expect 3 connections total
            .setNumberOfMessages(2); // We expect a total of 2 types of protocol messages to be exchanged in this
                                     // channel

    // This UDP channel is used for the resolver protocol that will convert the
    // public keys to dialing information (IP address + port)
    var udpBuilder = UDPBabelChannel.builder()
            .setEventLoopGroup(eventLoop);

    var resolver = new LocalNetworkDiscoveryProtocol(nodeStore);
    var disseminationApp = new AutomatedDisseminationAppFactory().createInstance(properties);
    var broadcastProtocol = new CrashFaultReliableBroadcastProtocol();
    var membershipProtocol = new StaticMembershipProtocol(membership);

    // The builder has two overloads, one of them using the default expected values
    // and another one where you can specify. It is prefered to specify what to
    // expect to the framework to correctly dimention the underlying data structures
    // and ensure maximum performance and minimal memory usage.
    Babel.builder(3, 4, 1, 4, 2)
            .setNodeStore(nodeStore)
            // We register the extensions that provide more optional functionality to Babel.
            // The extensions must be implemented by one protocol.
            // In this case, we are registering extensions for membership, broadcast, signed
            // messages, and resolver (for the public key -> address translation)
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
