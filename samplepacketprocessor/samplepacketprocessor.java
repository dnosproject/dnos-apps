package samplepacketprocessor;

import config.ConfigService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onosproject.grpc.grpcintegration.models.EventNotificationGrpc;
import org.onosproject.grpc.grpcintegration.models.EventNotificationGrpc.EventNotificationStub;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.Notification;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.RegistrationRequest;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.RegistrationResponse;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.Topic;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.topicType;
import org.onosproject.grpc.net.packet.models.PacketContextProtoOuterClass.PacketContextProto;

public class samplepacketprocessor {
  private static Logger log = Logger.getLogger(samplepacketprocessor.class);

  static String serverId = null;
  static String clientId = "samplepacketprocessor";

  public static void main(String[] args) {

    ManagedChannel channel;
    ConfigService configService = new ConfigService();
    configService.init();

    EventNotificationStub packetNotificationStub;

    // Create a managed gRPC channel.
    channel = ManagedChannelBuilder
            .forAddress("127.0.0.1", 50051)
            .usePlaintext()
            .build();

    packetNotificationStub = EventNotificationGrpc.newStub(channel);


    RegistrationRequest request = RegistrationRequest
            .newBuilder()
            .setClientId(clientId)
            .build();
    // Registers the client
    packetNotificationStub.register(
        request, new StreamObserver<RegistrationResponse>() {
          @Override
          public void onNext(RegistrationResponse value) {
            serverId = value.getServerId();
          }

          @Override
          public void onError(Throwable t) {}

          @Override
          public void onCompleted() {}
        });

    // Creates a packet event topic
    Topic packettopic =
        Topic.newBuilder()
                .setClientId(clientId)
                .setType(topicType.PACKET_EVENT)
                .build();

    // Implements a packet processor
    class PacketEvent implements Runnable {

      @Override
      public void run() {

        packetNotificationStub.onEvent(
            packettopic, new StreamObserver<Notification>() {
              @Override
              public void onNext(Notification value) {

                PacketContextProto packetContextProto = value.getPacketContext();
                PacketContextProto finalPacketContextProto = packetContextProto;

                byte[] packetByteArray =
                    finalPacketContextProto.getInboundPacket().getData().toByteArray();
                Ethernet eth = new Ethernet();

                try {
                  eth =
                      Ethernet.deserializer()
                          .deserialize(packetByteArray, 0, packetByteArray.length);
                } catch (DeserializationException e) {
                  e.printStackTrace();
                }

                if (eth == null) {
                  return;
                }

                long type = eth.getEtherType();
                if(Ethernet.TYPE_IPV4 == type) {
                    log.info("An IPv4 Packet has been received");
                }

              }

              @Override
              public void onError(Throwable t) {}

              @Override
              public void onCompleted() {
                log.info("completed");
              }
            });

        while (true) {}
      }
    }

    // Creates an instance of internal packet event class.
    PacketEvent packetEvent = new PacketEvent();
    Thread t = new Thread(packetEvent);
    t.start();
  }
}
