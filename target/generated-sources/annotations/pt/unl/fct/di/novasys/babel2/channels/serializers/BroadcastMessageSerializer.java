package pt.unl.fct.di.novasys.babel2.channels.serializers;

import com.google.auto.service.AutoService;
import io.netty.buffer.ByteBuf;
import java.lang.Class;
import java.lang.SuppressWarnings;
import java.util.UUID;
import messages.BroadcastMessage;
import pt.unl.fct.di.novasys.babel2.channels.BabelMessage;
import pt.unl.fct.di.novasys.babel2.channels.NodeStore;

@SuppressWarnings("rawTypes")
@AutoService(BabelNetworkMessageSerializer.class)
public final class BroadcastMessageSerializer implements BabelNetworkMessageSerializer<BroadcastMessage> {
  private static BabelNetworkSerializer<UUID> SERIALIZER_135956632;

  private static BabelNetworkSerializer<BabelMessage> SERIALIZER_232834050;

  public int getId() {
    return 2;
  }

  public Class getObjectClass() {
    return BroadcastMessage.class;
  }

  public void serialize(BroadcastMessage message, ByteBuf out, NodeStore store) {
    if (SERIALIZER_135956632 == null) {
      SERIALIZER_135956632 = SerializerRegistry.getSerializerByClass(UUID.class);
    }
    SERIALIZER_135956632.serialize(message.id(), out, store);
    if (SERIALIZER_232834050 == null) {
      SERIALIZER_232834050 = SerializerRegistry.getMessageSerializerByClass(BabelMessage.class);
    }
    SERIALIZER_232834050.serialize(message.message(), out, store);
  }

  public BroadcastMessage deserialize(ByteBuf buffer, NodeStore store) {
    if (SERIALIZER_135956632 == null) {
      SERIALIZER_135956632 = SerializerRegistry.getSerializerByClass(UUID.class);
    }
    UUID id = SERIALIZER_135956632.deserialize(buffer, store);
    if (SERIALIZER_232834050 == null) {
      SERIALIZER_232834050 = SerializerRegistry.getMessageSerializerByClass(BabelMessage.class);
    }
    BabelMessage message = SERIALIZER_232834050.deserialize(buffer, store);
    return new BroadcastMessage(id, message);
  }

  public int getEstimateSize(BroadcastMessage message) {
    return 0 + (SERIALIZER_135956632 != null ? SERIALIZER_135956632.getEstimateSize(message.id()) : SerializerRegistry.getSerializerByClass(UUID.class).getEstimateSize(message.id())) + (SERIALIZER_232834050 != null ? SERIALIZER_232834050.getEstimateSize(message.message()) : SerializerRegistry.getMessageSerializerByClass(BabelMessage.class).getEstimateSize(message.message()));
  }
}
