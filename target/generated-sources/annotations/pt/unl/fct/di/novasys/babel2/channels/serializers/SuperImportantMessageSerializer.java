package pt.unl.fct.di.novasys.babel2.channels.serializers;

import com.google.auto.service.AutoService;
import io.netty.buffer.ByteBuf;
import java.lang.Class;
import java.lang.String;
import java.lang.SuppressWarnings;
import messages.SuperImportantMessage;
import pt.unl.fct.di.novasys.babel2.channels.NodeStore;

@SuppressWarnings("rawTypes")
@AutoService(BabelNetworkMessageSerializer.class)
public final class SuperImportantMessageSerializer implements BabelNetworkMessageSerializer<SuperImportantMessage> {
  private static BabelNetworkSerializer<String> SERIALIZER_1559472121;

  public int getId() {
    return 1;
  }

  public Class getObjectClass() {
    return SuperImportantMessage.class;
  }

  public void serialize(SuperImportantMessage message, ByteBuf out, NodeStore store) {
    if (SERIALIZER_1559472121 == null) {
      SERIALIZER_1559472121 = SerializerRegistry.getSerializerByClass(String.class);
    }
    SERIALIZER_1559472121.serialize(message.text(), out, store);
  }

  public SuperImportantMessage deserialize(ByteBuf buffer, NodeStore store) {
    if (SERIALIZER_1559472121 == null) {
      SERIALIZER_1559472121 = SerializerRegistry.getSerializerByClass(String.class);
    }
    String text = SERIALIZER_1559472121.deserialize(buffer, store);
    return new SuperImportantMessage(text);
  }

  public int getEstimateSize(SuperImportantMessage message) {
    return 0 + (SERIALIZER_1559472121 != null ? SERIALIZER_1559472121.getEstimateSize(message.text()) : SerializerRegistry.getSerializerByClass(String.class).getEstimateSize(message.text()));
  }
}
