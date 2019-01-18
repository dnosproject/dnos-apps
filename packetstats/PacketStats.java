package packetstats;

import api.flowservice.Flow;
import api.flowservice.FlowAction;
import api.flowservice.FlowActionType;
import api.flowservice.FlowMatch;
import api.topostore.TopoSwitch;
import config.ConfigService;
import drivers.controller.Controller;
import drivers.onos.OnosController;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;

import org.onosproject.grpc.net.models.EventNotificationGrpc;
import org.onosproject.grpc.net.models.PacketOutServiceGrpc;
import org.onosproject.grpc.net.models.ServicesProto;
import org.onosproject.grpc.net.packet.models.InboundPacketProtoOuterClass;
import org.onosproject.grpc.net.packet.models.PacketContextProtoOuterClass;



import java.util.ArrayList;
import java.util.Set;

public class PacketStats {
  private static Logger log = Logger.getLogger(PacketStats.class);

  static String serverId = null;
  static String clientId = "packetStats";
  private static int TABLE_ID = 0;
  private static int TABLE_ID_CTRL_PACKETS = 0;
  private static int CTRL_PACKET_PRIORITY = 100;
  private static int IP_PACKET_PRIORITY = 1000;
  private static int DEFAULT_TIMEOUT = 10;


    public static void main(String[] args) {

    ManagedChannel channel;
    String controllerName;
    String controllerIP;
    String grpcPort;
    Controller controller = null;



    ConfigService configService = new ConfigService();
    configService.init();

    controllerName = configService.getConfig().getControllerName();
    controllerIP = configService.getConfig().getControllerIp();

    grpcPort = configService.getConfig().getGrpcPort();
    controller = new OnosController();

    EventNotificationGrpc.EventNotificationStub packetNotificationStub;

    channel =
        ManagedChannelBuilder.forAddress(controllerIP, Integer.parseInt(grpcPort))
            .usePlaintext()
            .build();

    packetNotificationStub = EventNotificationGrpc.newStub(channel);



    Controller finalController = controller;
    Set<TopoSwitch> topoSwitches = finalController.topoStore.getSwitches();

    for (TopoSwitch topoSwitch : topoSwitches) {
      FlowMatch flowMatch = FlowMatch.builder().ethType(2048).build();

      FlowAction flowAction = new FlowAction(FlowActionType.CONTROLLER, 0);

      ArrayList<FlowAction> flowActions = new ArrayList<FlowAction>();
      flowActions.add(flowAction);

      Flow flow =
          Flow.builder()
              .deviceID(topoSwitch.getSwitchID())
              .tableID(TABLE_ID_CTRL_PACKETS)
              .flowMatch(flowMatch)
              .flowActions(flowActions)
              .priority(CTRL_PACKET_PRIORITY)
              .appId("PacketStats")
              .isPermanent(true)
              .timeOut(0)
              .build();

      finalController.flowService.addFlow(flow);

      flowMatch = FlowMatch.builder().ethType(Ethernet.TYPE_ARP).build();

      flow =
          Flow.builder()
              .deviceID(topoSwitch.getSwitchID())
              .tableID(TABLE_ID_CTRL_PACKETS)
              .flowMatch(flowMatch)
              .flowActions(flowActions)
              .priority(CTRL_PACKET_PRIORITY)
              .appId("PacketStats")
              .isPermanent(true)
              .timeOut(0)
              .build();

      finalController.flowService.addFlow(flow);
    }

    ServicesProto.RegistrationRequest request =
        ServicesProto.RegistrationRequest.newBuilder().setClientId(clientId).build();
    packetNotificationStub.register(
        request,
        new StreamObserver<ServicesProto.RegistrationResponse>() {
          @Override
          public void onNext(ServicesProto.RegistrationResponse value) {

            serverId = value.getServerId();
          }

          @Override
          public void onError(Throwable t) {}

          @Override
          public void onCompleted() {}
        });

    ServicesProto.Topic topic =
        ServicesProto.Topic.newBuilder()
            .setClientId(clientId)
            .setType(ServicesProto.topicType.PACKET_EVENT)
            .build();

    class Event implements Runnable {

      @Override
      public void run() {

        packetNotificationStub.onEvent(
            topic,
            new StreamObserver<ServicesProto.Notification>() {
              @Override
              public void onNext(ServicesProto.Notification value) {

                PacketContextProtoOuterClass.PacketContextProto packetContextProto =
                    value.getPacketContext();

                PacketContextProtoOuterClass.PacketContextProto finalPacketContextProto =
                    packetContextProto;
                byte[] packetByteArray =
                    finalPacketContextProto.getInboundPacket().getData().toByteArray();
                InboundPacketProtoOuterClass.InboundPacketProto inboundPacketProto =
                    finalPacketContextProto.getInboundPacket();
                Ethernet eth = new Ethernet();
                // log.info(inboundPacketProto.getConnectPoint().getDeviceId());

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


                short ethType = eth.getEtherType();

                if(ethType ==  Ethernet.TYPE_IPV4)
                {

                    log.info("IPv4 Packet");
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

    Event event = new Event();
    Thread t = new Thread(event);
    t.start();
  }
}
