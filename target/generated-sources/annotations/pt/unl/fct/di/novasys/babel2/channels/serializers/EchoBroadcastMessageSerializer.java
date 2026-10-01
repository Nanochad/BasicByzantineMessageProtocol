package pt.unl.fct.di.novasys.babel2.channels.serializers;

import com.google.auto.service.AutoService;
import io.netty.buffer.ByteBuf;
import java.lang.Class;
import java.lang.SuppressWarnings;
import messages.BroadcastMessage;
import messages.EchoBroadcastMessage;
import pt.unl.fct.di.novasys.babel2.channels.NodeStore;
import pt.unl.fct.di.novasys.babel2.channels.SignedBabelMessage;

@SuppressWarnings("rawTypes")
@AutoService(BabelNetworkMessageSerializer.class)
public final class EchoBroadcastMessageSerializer implements BabelNetworkMessageSerializer<EchoBroadcastMessage> {
  private static BabelNetworkSerializer<SignedBabelMessage<BroadcastMessage>> SERIALIZER_1231445906;

  public int getId() {
    return 3;
  }

  public Class getObjectClass() {
    return EchoBroadcastMessage.class;
  }

  public void serialize(EchoBroadcastMessage message, ByteBuf out, NodeStore store) {
    if (SERIALIZER_1231445906 == null) {
      SERIALIZER_1231445906 = SerializerRegistry.getSignedBabelMessageSerializerByClass(BroadcastMessage.class);
    }
    SERIALIZER_1231445906.serialize(message.message(), out, store);
  }

  public EchoBroadcastMessage deserialize(ByteBuf buffer, NodeStore store) {
    if (SERIALIZER_1231445906 == null) {
      SERIALIZER_1231445906 = SerializerRegistry.getSignedBabelMessageSerializerByClass(BroadcastMessage.class);
    }
    SignedBabelMessage<BroadcastMessage> message = SERIALIZER_1231445906.deserialize(buffer, store);
    return new EchoBroadcastMessage(message);
  }

  public int getEstimateSize(EchoBroadcastMessage message) {
    return 0 + (SERIALIZER_1231445906 != null ? SERIALIZER_1231445906.getEstimateSize(message.message()) : SerializerRegistry.getSignedBabelMessageSerializerByClass(BroadcastMessage.class).getEstimateSize(message.message()));
  }
}
