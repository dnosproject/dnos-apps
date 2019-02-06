package fwd;

import api.flowservice.Flow;
import api.flowservice.FlowAction;
import api.flowservice.FlowActionType;
import api.flowservice.FlowMatch;
import api.topostore.TopoEdge;
import api.topostore.TopoHost;
import api.topostore.TopoSwitch;
import com.google.protobuf.ByteString;
import config.ConfigService;
import drivers.controller.Controller;
import drivers.onos.OnosController;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onlab.packet.*;
import org.onosproject.grpc.net.flow.instructions.models.InstructionProtoOuterClass.InstructionProto;
import org.onosproject.grpc.net.flow.instructions.models.InstructionProtoOuterClass.OutputInstructionProto;
import org.onosproject.grpc.net.flow.models.FlowRuleProto;
import org.onosproject.grpc.net.flow.models.TrafficTreatmentProtoOuterClass;
import org.onosproject.grpc.net.models.EventNotificationGrpc;
import org.onosproject.grpc.net.models.PortProtoOuterClass;
import org.onosproject.grpc.net.models.ServicesProto;
import org.onosproject.grpc.net.models.PacketOutServiceGrpc;
import org.onosproject.grpc.net.packet.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class fwd {
  private static Logger log = Logger.getLogger(fwd.class);

  static String serverId = null;
  static String clientId = "fwd";
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
    EventNotificationGrpc.EventNotificationStub linkEventNotification;


    PacketOutServiceGrpc.PacketOutServiceStub packetOutServiceStub;
    channel =
        ManagedChannelBuilder.forAddress(controllerIP, Integer.parseInt(grpcPort))
            .usePlaintext()
            .build();

    packetNotificationStub = EventNotificationGrpc.newStub(channel);

    packetOutServiceStub = PacketOutServiceGrpc.newStub(channel);

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
                          .appId("ReactiveFwd")
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
                          .appId("ReactiveFwd")
                          .isPermanent(true)
                          .timeOut(0)
                          .build();

          finalController.flowService.addFlow(flow);
      }


    ServicesProto.RegistrationRequest request =
        ServicesProto.RegistrationRequest
                .newBuilder()
                .setClientId(clientId)
                .build();
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


    ServicesProto.Topic packettopic =
        ServicesProto.Topic.newBuilder()
            .setClientId(clientId)
            .setType(ServicesProto.topicType.PACKET_EVENT)
            .build();



    class PacketEvent implements Runnable {

      @Override
      public void run() {

        packetNotificationStub.onEvent(packettopic
            ,
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

                // Handle ARP packets

                if (type == Ethernet.TYPE_ARP) {
                  ARP arpPacket = (ARP) eth.getPayload();
                  Ip4Address targetIpAddress =
                      Ip4Address.valueOf(arpPacket.getTargetProtocolAddress());

                  String dstMac =
                      finalController.topoStore.getTopoHostByIP(targetIpAddress).getHostMac();

                  if (dstMac == null) {
                    return;
                  }

                  Ethernet ethReply =
                      ARP.buildArpReply(targetIpAddress, MacAddress.valueOf(dstMac), eth);


                  OutputInstructionProto outputInstructionProto =
                          OutputInstructionProto
                                  .newBuilder()
                                  .setPort(PortProtoOuterClass.PortProto.newBuilder()
                                  .setPortNumber(
                                          inboundPacketProto.getConnectPoint().getPortNumber())
                                  .build()).build();
                  InstructionProto instructionProto =
                      InstructionProto.newBuilder()
                          .setOutput(outputInstructionProto)
                          .build();

                  TrafficTreatmentProtoOuterClass.TrafficTreatmentProto trafficTreatmentProto =
                      TrafficTreatmentProtoOuterClass.TrafficTreatmentProto.newBuilder()
                          .addAllInstructions(instructionProto)
                          .build();

                  OutboundPacketProtoOuterClass.OutboundPacketProto outboundPacketProto =
                      OutboundPacketProtoOuterClass.OutboundPacketProto.newBuilder()
                          .setDeviceId(inboundPacketProto.getConnectPoint().getDeviceId())
                          .setTreatment(trafficTreatmentProto)
                          .setData(ByteString.copyFrom(ethReply.serialize()))
                          .build();

                  packetOutServiceStub.emit(
                      outboundPacketProto,
                      new StreamObserver<ServicesProto.PacketOutStatus>() {
                        @Override
                        public void onNext(ServicesProto.PacketOutStatus value) {}

                        @Override
                        public void onError(Throwable t) {}

                        @Override
                        public void onCompleted() {}
                      });

                  return;
                }

                if (type == Ethernet.TYPE_IPV4) {

                    log.info("PACKET_IN Event");
                  IPv4 IPv4packet = (IPv4) eth.getPayload();
                  byte ipv4Protocol = IPv4packet.getProtocol();

                  if (!finalController.topoStore.checkHostExistenceWithMac(eth.getSourceMAC())
                      || !finalController.topoStore.checkHostExistenceWithMac(
                          eth.getDestinationMAC())) {
                    return;
                  }

                  TopoHost srcHost = finalController.topoStore.getTopoHostByMac(eth.getSourceMAC());
                  TopoHost dstHost =
                      finalController.topoStore.getTopoHostByMac(eth.getDestinationMAC());
                  FlowMatch flowMatchFwd;

                  if (srcHost == null || dstHost == null) {
                    return;
                  }

                  if (inboundPacketProto
                      .getConnectPoint()
                      .getDeviceId()
                      .equals(dstHost.getHostLocation().getElementID())) {

                    flowMatchFwd =
                        FlowMatch.builder()
                            .ethSrc(srcHost.getHostMac())
                            .ethDst(dstHost.getHostMac())
                            // .ipv4Src(srcHost.getHostIPAddresses().get(0) + "/32")
                            // .ipv4Dst(dstHost.getHostIPAddresses().get(0) + "/32")
                            .ethType(Ethernet.TYPE_IPV4)
                            .build();

                    FlowAction flowAction =
                        new FlowAction(
                            FlowActionType.OUTPUT,
                            Integer.parseInt(dstHost.getHostLocation().getPort()));

                    ArrayList<FlowAction> flowActions = new ArrayList<FlowAction>();
                    flowActions.add(flowAction);

                    Flow flow =
                        Flow.builder()
                            .deviceID(dstHost.getHostLocation().getElementID())
                            .tableID(TABLE_ID)
                            .flowMatch(flowMatchFwd)
                            .flowActions(flowActions)
                            .priority(IP_PACKET_PRIORITY)
                            .appId("ReactiveFwd")
                            .timeOut(DEFAULT_TIMEOUT)
                            .build();

                    finalController.flowService.addFlow(flow);

                    OutputInstructionProto outputInstructionProto =
                              OutputInstructionProto
                                      .newBuilder()
                                      .setPort(PortProtoOuterClass.PortProto.newBuilder()
                                              .setPortNumber(dstHost.getHostLocation().getPort())
                                              .build()).build();

                    InstructionProto instructionProto =
                        InstructionProto.newBuilder()
                                .setOutput(outputInstructionProto)
                            .build();

                    TrafficTreatmentProtoOuterClass.TrafficTreatmentProto trafficTreatmentProto =
                        TrafficTreatmentProtoOuterClass.TrafficTreatmentProto.newBuilder()
                            .addAllInstructions(instructionProto)
                            .build();

                    OutboundPacketProtoOuterClass.OutboundPacketProto outboundPacketProto2 =
                        OutboundPacketProtoOuterClass.OutboundPacketProto.newBuilder()
                            .setDeviceId(inboundPacketProto.getConnectPoint().getDeviceId())
                            .setTreatment(trafficTreatmentProto)
                            .setData(inboundPacketProto.getData())
                            .build();

                    packetOutServiceStub.emit(
                        outboundPacketProto2,
                        new StreamObserver<ServicesProto.PacketOutStatus>() {
                          @Override
                          public void onNext(ServicesProto.PacketOutStatus value) {

                          }

                          @Override
                          public void onError(Throwable t) {}

                          @Override
                          public void onCompleted() {}
                        });

                    return;
                  }

                  List<TopoEdge> path = null;
                  path =
                      finalController.topoStore.getShortestPath(
                          inboundPacketProto.getConnectPoint().getDeviceId(),
                          dstHost.getHostLocation().getElementID());

                  TopoEdge firstEdge = path.get(0);
                  flowMatchFwd =
                      FlowMatch.builder()
                          .ethSrc(srcHost.getHostMac())
                          .ethDst(dstHost.getHostMac())
                          // .ipv4Src(srcHost.getHostIPAddresses().get(0) + "/32")
                          // .ipv4Dst(dstHost.getHostIPAddresses().get(0) + "/32")
                          .ethType(Ethernet.TYPE_IPV4)
                          .build();

                  FlowAction flowAction =
                      new FlowAction(
                          FlowActionType.OUTPUT, Integer.parseInt(firstEdge.getSrcPort()));

                  ArrayList<FlowAction> flowActions = new ArrayList<FlowAction>();
                  flowActions.add(flowAction);

                  Flow flow =
                      Flow.builder()
                          .deviceID(firstEdge.getSrc())
                          .tableID(TABLE_ID)
                          .flowMatch(flowMatchFwd)
                          .flowActions(flowActions)
                          .priority(IP_PACKET_PRIORITY)
                          .appId("ReactiveFwd")
                          .timeOut(DEFAULT_TIMEOUT)
                          .build();

                  finalController.flowService.addFlow(flow);




                    OutputInstructionProto outputInstructionProto =
                            OutputInstructionProto
                                    .newBuilder()
                                    .setPort(PortProtoOuterClass.PortProto.newBuilder()
                                            .setPortNumber(firstEdge.getSrcPort())
                                            .build()).build();


                  InstructionProto instructionProto =
                      InstructionProto.newBuilder()
                          .setOutput(outputInstructionProto)
                          .build();

                  TrafficTreatmentProtoOuterClass.TrafficTreatmentProto trafficTreatmentProto =
                      TrafficTreatmentProtoOuterClass.TrafficTreatmentProto.newBuilder()
                          .addAllInstructions(instructionProto)
                          .build();

                  OutboundPacketProtoOuterClass.OutboundPacketProto outboundPacketProto2 =
                      OutboundPacketProtoOuterClass.OutboundPacketProto.newBuilder()
                          .setDeviceId(inboundPacketProto.getConnectPoint().getDeviceId())
                          .setTreatment(trafficTreatmentProto)
                          .setData(inboundPacketProto.getData())
                          .build();

                  packetOutServiceStub.emit(
                      outboundPacketProto2,
                      new StreamObserver<ServicesProto.PacketOutStatus>() {
                        @Override
                        public void onNext(ServicesProto.PacketOutStatus value) {
                            log.info(value);
                        }

                        @Override
                        public void onError(Throwable t) {}

                        @Override
                        public void onCompleted() {}
                      });
                }
              }

              @Override
              public void onError(Throwable t) {}

              @Override
              public void onCompleted() {
                log.info("completed");
              }
            });







        while (true) {
            

        }
      }
    }








    PacketEvent packetEvent= new PacketEvent();
    Thread t = new Thread(packetEvent);
    t.start();
  }
}
