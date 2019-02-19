package fwdgrpc;

import api.topostore.TopoEdge;
import api.topostore.TopoHost;
import api.topostore.TopoSwitch;
import com.google.protobuf.ByteString;
import config.ConfigService;
import drivers.controller.Controller;
import drivers.onos.OnosController;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onlab.packet.ARP;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.grpc.grpcintegration.models.ControlMessagesProto.Empty;
import org.onosproject.grpc.grpcintegration.models.EventNotificationGrpc;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.Notification;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.RegistrationRequest;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.RegistrationResponse;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.Topic;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.topicType;
import org.onosproject.grpc.grpcintegration.models.FlowServiceGrpc;
import org.onosproject.grpc.grpcintegration.models.PacketOutServiceGrpc;
import org.onosproject.grpc.grpcintegration.models.StatusProto;
import org.onosproject.grpc.grpcintegration.models.StatusProto.FlowServiceStatus;
import org.onosproject.grpc.grpcintegration.models.StatusProto.PacketOutStatus;
import org.onosproject.grpc.grpcintegration.models.TopoServiceGrpc;
import org.onosproject.grpc.net.flow.criteria.models.CriterionProtoOuterClass;
import org.onosproject.grpc.net.flow.criteria.models.CriterionProtoOuterClass.CriterionProto;
import org.onosproject.grpc.net.flow.criteria.models.CriterionProtoOuterClass.EthTypeCriterionProto;
import org.onosproject.grpc.net.flow.instructions.models.InstructionProtoOuterClass.InstructionProto;
import org.onosproject.grpc.net.flow.instructions.models.InstructionProtoOuterClass.OutputInstructionProto;
import org.onosproject.grpc.net.flow.models.FlowRuleProto;
import org.onosproject.grpc.net.flow.models.TrafficSelectorProtoOuterClass.TrafficSelectorProto;
import org.onosproject.grpc.net.flow.models.TrafficTreatmentProtoOuterClass.TrafficTreatmentProto;
import org.onosproject.grpc.net.models.PortProtoOuterClass;
import org.onosproject.grpc.net.packet.models.InboundPacketProtoOuterClass.InboundPacketProto;
import org.onosproject.grpc.net.packet.models.OutboundPacketProtoOuterClass.OutboundPacketProto;
import org.onosproject.grpc.net.packet.models.PacketContextProtoOuterClass.PacketContextProto;
import org.onosproject.grpc.net.topology.models.TopologyEdgeProtoOuterClass.TopologyEdgeProto;
import org.onosproject.grpc.net.topology.models.TopologyGraphProtoOuterClass.TopologyGraphProto;
import org.onosproject.grpc.net.topology.models.TopologyVertexProtoOuterClass.TopologyVertexProto;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

public class fwdgrpc {
  private static Logger log = Logger.getLogger(fwdgrpc.class);

  static String serverId = null;
  static String clientId = "fwd";
  private static int TABLE_ID = 0;
  private static int TABLE_ID_CTRL_PACKETS = 0;
  private static int CTRL_PACKET_PRIORITY = 100;
  private static int IP_PACKET_PRIORITY = 1000;
  private static int DEFAULT_TIMEOUT = 10;
  private static List<TopologyEdgeProto> topologyEdgeProtoList;
  private static List<TopologyVertexProto> topologyVertexProtoList;

  public static byte[] createMacAddress (String mac) {

        byte[] macAddrBytes = new byte[6];

        String[] macAddressParts = mac.split(":");
        for(int i=0; i< macAddrBytes.length;i++) {

            Integer hex = Integer.parseInt(macAddressParts[i], 16);
            macAddrBytes[i] = hex.byteValue();
        }
        return macAddrBytes;
      }

  public static  byte[] createIpAddress (String ip) {

      InetAddress ipAddr = null;

      try {
          ipAddr = InetAddress.getByName(ip);
      } catch (UnknownHostException e) {
          e.printStackTrace();
      }

      log.info(ipAddr.getAddress());

      return ipAddr.getAddress();

  }

  public static void main(String[] args) {

    ManagedChannel channel;
    final String CONTROLLER_PORT = "CONTROLLER";
    String controllerIP;
    String grpcPort;
    Controller controller = null;
    ConfigService configService = new ConfigService();
    configService.init();

    controllerIP = configService.getConfig().getControllerIp();

    grpcPort = configService.getConfig().getGrpcPort();
    controller = new OnosController();

    EventNotificationGrpc.EventNotificationStub packetNotificationStub;
      FlowServiceGrpc.FlowServiceStub flowServiceStub;
      TopoServiceGrpc.TopoServiceStub topoServiceStub;


    PacketOutServiceGrpc.PacketOutServiceStub packetOutServiceStub;
    channel =
        ManagedChannelBuilder.forAddress(controllerIP, Integer.parseInt(grpcPort))
            .usePlaintext()
            .build();

    packetNotificationStub = EventNotificationGrpc.newStub(channel);
    topoServiceStub = TopoServiceGrpc.newStub(channel);

    flowServiceStub = FlowServiceGrpc.newStub(channel);

    packetOutServiceStub = PacketOutServiceGrpc.newStub(channel);
    Empty req = Empty.newBuilder().build();



    topoServiceStub.getGraph(req, new StreamObserver<TopologyGraphProto>() {
        @Override
        public void onNext(TopologyGraphProto value) {
            topologyEdgeProtoList = value.getEdgesList();
            topologyVertexProtoList = value.getVertexesList();
        }

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
    });


      Controller finalController = controller;
      Set<TopoSwitch> topoSwitches = finalController.topoStore.getSwitches();



      for (TopologyVertexProto topologyVertexProto: topologyVertexProtoList) {

          EthTypeCriterionProto ethTypeCriterionProto = EthTypeCriterionProto
                  .newBuilder()
                  .setEthType(Ethernet.TYPE_IPV4)
                  .build();

      CriterionProto criterionProto =
          CriterionProto.newBuilder()
              .setEthTypeCriterion(ethTypeCriterionProto)
              .setType(CriterionProtoOuterClass.TypeProto.ETH_TYPE)
              .build();


          TrafficSelectorProto trafficSelectorProto = TrafficSelectorProto
                  .newBuilder()
                  .addCriterion(criterionProto)
                  .build();

          InstructionProto instructionProto = InstructionProto
                  .newBuilder()
                  .setOutput(OutputInstructionProto
                          .newBuilder()
                          .setPort(PortProtoOuterClass
                                  .PortProto
                                  .newBuilder()
                                  .setPortNumber(CONTROLLER_PORT)
                                  .build())
                          .build())
                  .build();

          TrafficTreatmentProto trafficTreatmentProto = TrafficTreatmentProto
                  .newBuilder()
                  .addAllInstructions(instructionProto)
                  .build();


          FlowRuleProto flowRuleProto = FlowRuleProto
                  .newBuilder()
                  .setTreatment(trafficTreatmentProto)
                  .setSelector(trafficSelectorProto)
                  .setPriority(CTRL_PACKET_PRIORITY)
                  .setDeviceId(topologyVertexProto.getDeviceId().getDeviceId())
                  .setTableId(TABLE_ID_CTRL_PACKETS)
                  .setTimeout(0)
                  .setPermanent(true)
                  .setAppName("Reactive_fwd")
                  .build();

          flowServiceStub.addFlow(flowRuleProto, new StreamObserver<StatusProto.FlowServiceStatus>() {
              @Override
              public void onNext(StatusProto.FlowServiceStatus value) {
              }

              @Override
              public void onError(Throwable t) {

              }

              @Override
              public void onCompleted() {

              }
          });

          ethTypeCriterionProto = EthTypeCriterionProto
                  .newBuilder()
                  .setEthType(Ethernet.TYPE_ARP)
                  .build();

          criterionProto =
                  CriterionProto.newBuilder()
                          .setEthTypeCriterion(ethTypeCriterionProto)
                          .setType(CriterionProtoOuterClass.TypeProto.ETH_TYPE)
                          .build();

          trafficSelectorProto = TrafficSelectorProto
                  .newBuilder()
                  .addCriterion(criterionProto)
                  .build();


          flowRuleProto = FlowRuleProto
                  .newBuilder()
                  .setTreatment(trafficTreatmentProto)
                  .setSelector(trafficSelectorProto)
                  .setPriority(CTRL_PACKET_PRIORITY)
                  .setDeviceId(topologyVertexProto.getDeviceId().getDeviceId())
                  .setTableId(TABLE_ID_CTRL_PACKETS)
                  .setTimeout(0)
                  .setPermanent(true)
                  .setAppName("Reactive_fwd")
                  .build();

          flowServiceStub.addFlow(flowRuleProto, new StreamObserver<FlowServiceStatus>() {
              @Override
              public void onNext(FlowServiceStatus value) {
                  log.info(value);
                  }

              @Override
              public void onError(Throwable t) {}

              @Override
              public void onCompleted() {}
          });

      }

    RegistrationRequest request =
        RegistrationRequest
                .newBuilder()
                .setClientId(clientId)
                .build();
    packetNotificationStub.register(
        request,
        new StreamObserver<RegistrationResponse>() {
          @Override
          public void onNext(RegistrationResponse value) {
            serverId = value.getServerId();
          }

          @Override
          public void onError(Throwable t) {}

          @Override
          public void onCompleted() {}
        });

    Topic packettopic =
        Topic.newBuilder()
            .setClientId(clientId)
            .setType(topicType.PACKET_EVENT)
            .build();


    class PacketEvent implements Runnable {

      @Override
      public void run() {

        packetNotificationStub.onEvent(
            packettopic,
            new StreamObserver<Notification>() {
              @Override
              public void onNext(Notification value) {

                PacketContextProto packetContextProto = value.getPacketContext();

                PacketContextProto finalPacketContextProto = packetContextProto;
                byte[] packetByteArray =
                    finalPacketContextProto.getInboundPacket().getData().toByteArray();
                InboundPacketProto inboundPacketProto = finalPacketContextProto.getInboundPacket();
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
                      OutputInstructionProto.newBuilder()
                          .setPort(
                              PortProtoOuterClass.PortProto.newBuilder()
                                  .setPortNumber(
                                      inboundPacketProto.getConnectPoint().getPortNumber())
                                  .build())
                          .build();
                  InstructionProto instructionProto =
                      InstructionProto.newBuilder()
                              .setOutput(outputInstructionProto)
                              .build();

                  TrafficTreatmentProto trafficTreatmentProto =
                      TrafficTreatmentProto.newBuilder()
                          .addAllInstructions(instructionProto)
                          .build();

                  OutboundPacketProto outboundPacketProto =
                      OutboundPacketProto.newBuilder()
                          .setDeviceId(inboundPacketProto.getConnectPoint().getDeviceId())
                          .setTreatment(trafficTreatmentProto)
                          .setData(ByteString.copyFrom(ethReply.serialize()))
                          .build();

                  packetOutServiceStub.emit(
                      outboundPacketProto,
                      new StreamObserver<PacketOutStatus>() {
                        @Override
                        public void onNext(PacketOutStatus value) {}

                        @Override
                        public void onError(Throwable t) {}

                        @Override
                        public void onCompleted() {}
                      });

                  return;
                }

                if (type == Ethernet.TYPE_IPV4) {

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

                  if (srcHost == null || dstHost == null) {
                    return;
                  }

                  if (inboundPacketProto.getConnectPoint()
                      .getDeviceId().equals(dstHost
                                  .getHostLocation().getElementID())) {

                    CriterionProto ethSrcCriterion =
                        CriterionProto.newBuilder()
                            .setEthCriterion(
                                CriterionProtoOuterClass.EthCriterionProto.newBuilder()
                                    .setMacAddress(
                                        ByteString.copyFrom(createMacAddress(srcHost.getHostMac())))
                                    .build())
                            .setType(CriterionProtoOuterClass.TypeProto.ETH_SRC)
                            .build();

                    CriterionProto ethDstCriterion =
                        CriterionProto.newBuilder()
                            .setEthCriterion(
                                CriterionProtoOuterClass.EthCriterionProto.newBuilder()
                                    .setMacAddress(
                                        ByteString.copyFrom(createMacAddress(dstHost.getHostMac())))
                                    .build())
                            .setType(CriterionProtoOuterClass.TypeProto.ETH_DST)
                            .build();
                    CriterionProto ethTypeCriterion =
                        CriterionProto.newBuilder()
                            .setEthTypeCriterion(
                                EthTypeCriterionProto.newBuilder()
                                    .setEthType(Ethernet.TYPE_IPV4)
                                    .build())
                            .setType(CriterionProtoOuterClass.TypeProto.ETH_TYPE)
                            .build();

                    CriterionProto srcIpCriterion =
                        CriterionProto.newBuilder()
                                .setType(CriterionProtoOuterClass.TypeProto.IPV4_SRC)
                            .setIpCriterion(
                                CriterionProtoOuterClass.IPCriterionProto.newBuilder()
                                        .setPrefixLength(32)
                                    .setIpPrefix(
                                        ByteString.copyFrom(
                                            createIpAddress(srcHost.getHostIPAddresses().get(0))))
                                    .build())
                            .build();
                      CriterionProto dstIpCriterion =
                              CriterionProto.newBuilder()
                                      .setType(CriterionProtoOuterClass.TypeProto.IPV4_DST)
                                      .setIpCriterion(
                                              CriterionProtoOuterClass.IPCriterionProto.newBuilder()
                                                      .setPrefixLength(32)
                                                      .setIpPrefix(
                                                              ByteString.copyFrom(
                                                                      createIpAddress(dstHost.getHostIPAddresses().get(0))))
                                                      .build())
                                      .build();

                    TrafficSelectorProto trafficSelectorProto =
                        TrafficSelectorProto.newBuilder()
                            .addCriterion(ethSrcCriterion)
                            .addCriterion(ethDstCriterion)
                            .addCriterion(ethTypeCriterion)
                            .addCriterion(srcIpCriterion)
                                .addCriterion(dstIpCriterion)
                            .build();

                    InstructionProto instructionProto =
                        InstructionProto.newBuilder()
                            .setOutput(
                                OutputInstructionProto.newBuilder()
                                    .setPort(
                                        PortProtoOuterClass.PortProto.newBuilder()
                                            .setPortNumber(dstHost.getHostLocation().getPort())
                                            .build())
                                    .build())
                            .build();

                    TrafficTreatmentProto trafficTreatmentProto =
                        TrafficTreatmentProto.newBuilder()
                            .addAllInstructions(instructionProto)
                            .build();

                    FlowRuleProto flowRuleProto =
                        FlowRuleProto.newBuilder()
                            .setTreatment(trafficTreatmentProto)
                            .setSelector(trafficSelectorProto)
                            .setPriority(IP_PACKET_PRIORITY)
                            .setAppName("Reactive_Fwd")
                            .setDeviceId(dstHost.getHostLocation().getElementID())
                            .setTableId(TABLE_ID)
                            .setTimeout(DEFAULT_TIMEOUT)
                            .setPermanent(false)
                            .build();

                    flowServiceStub.addFlow(
                        flowRuleProto,
                        new StreamObserver<FlowServiceStatus>() {
                          @Override
                          public void onNext(FlowServiceStatus value) {}

                          @Override
                          public void onError(Throwable t) {}

                          @Override
                          public void onCompleted() {}
                        });

                    OutputInstructionProto outputInstructionProto =
                        OutputInstructionProto.newBuilder()
                            .setPort(
                                PortProtoOuterClass.PortProto.newBuilder()
                                    .setPortNumber(dstHost.getHostLocation().getPort())
                                    .build())
                            .build();

                    instructionProto =
                        InstructionProto.newBuilder().setOutput(outputInstructionProto).build();
                    trafficTreatmentProto =
                        TrafficTreatmentProto.newBuilder()
                            .addAllInstructions(instructionProto)
                            .build();

                    OutboundPacketProto outboundPacketProto2 =
                        OutboundPacketProto.newBuilder()
                            .setDeviceId(inboundPacketProto.getConnectPoint().getDeviceId())
                            .setTreatment(trafficTreatmentProto)
                            .setData(inboundPacketProto.getData())
                            .build();

                    packetOutServiceStub.emit(
                        outboundPacketProto2,
                        new StreamObserver<PacketOutStatus>() {
                          @Override
                          public void onNext(PacketOutStatus value) {}

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

                  CriterionProto ethSrcCriterion =
                      CriterionProto.newBuilder()
                          .setEthCriterion(
                              CriterionProtoOuterClass.EthCriterionProto.newBuilder()
                                  .setMacAddress(
                                      ByteString.copyFrom(createMacAddress(srcHost.getHostMac())))
                                  .build())
                          .setType(CriterionProtoOuterClass.TypeProto.ETH_SRC)
                          .build();

                  CriterionProto ethDstCriterion =
                      CriterionProto.newBuilder()
                          .setEthCriterion(
                              CriterionProtoOuterClass.EthCriterionProto.newBuilder()
                                  .setMacAddress(
                                      ByteString.copyFrom(createMacAddress(dstHost.getHostMac())))
                                  .build())
                          .setType(CriterionProtoOuterClass.TypeProto.ETH_DST)
                          .build();
                  CriterionProto ethTypeCriterion =
                      CriterionProto.newBuilder()
                          .setEthTypeCriterion(
                              EthTypeCriterionProto.newBuilder()
                                  .setEthType(Ethernet.TYPE_IPV4)
                                  .build())
                          .setType(CriterionProtoOuterClass.TypeProto.ETH_TYPE)
                          .build();

                  CriterionProto srcIpCriterion =
                      CriterionProto.newBuilder()
                          .setType(CriterionProtoOuterClass.TypeProto.IPV4_SRC)
                          .setIpCriterion(
                              CriterionProtoOuterClass.IPCriterionProto.newBuilder()
                                      .setPrefixLength(32)
                                  .setIpPrefix(
                                      ByteString.copyFrom(
                                          createIpAddress(srcHost.getHostIPAddresses().get(0))))
                                  .build())
                          .build();

                  CriterionProto dstIpCriterion =
                      CriterionProto.newBuilder()
                              .setType(CriterionProtoOuterClass.TypeProto.IPV4_DST)
                          .setIpCriterion(
                              CriterionProtoOuterClass.IPCriterionProto.newBuilder()
                                      .setPrefixLength(32)
                                  .setIpPrefix(
                                      ByteString.copyFrom(
                                          createIpAddress(dstHost.getHostIPAddresses().get(0))))
                                  .build())
                          .build();

                  TrafficSelectorProto trafficSelectorProto =
                      TrafficSelectorProto.newBuilder()
                          .addCriterion(ethSrcCriterion)
                          .addCriterion(ethDstCriterion)
                          .addCriterion(ethTypeCriterion)
                          .addCriterion(srcIpCriterion)
                          .addCriterion(dstIpCriterion)
                          .build();

                  InstructionProto instructionProto =
                      InstructionProto.newBuilder()
                          .setOutput(
                              OutputInstructionProto.newBuilder()
                                  .setPort(
                                      PortProtoOuterClass.PortProto.newBuilder()
                                          .setPortNumber(firstEdge.getSrcPort())
                                          .build())
                                  .build())
                          .build();

                  TrafficTreatmentProto trafficTreatmentProto =
                      TrafficTreatmentProto.newBuilder()
                          .addAllInstructions(instructionProto)
                          .build();

                  FlowRuleProto flowRuleProto =
                      FlowRuleProto.newBuilder()
                          .setTreatment(trafficTreatmentProto)
                          .setSelector(trafficSelectorProto)
                          .setPriority(IP_PACKET_PRIORITY)
                          .setDeviceId(firstEdge.getSrc())
                          .setTableId(TABLE_ID)
                          .setTimeout(DEFAULT_TIMEOUT)
                          .setPermanent(false)
                          .setAppName("Reactive_fwd")
                          .build();

                  flowServiceStub.addFlow(
                      flowRuleProto,
                      new StreamObserver<FlowServiceStatus>() {
                        @Override
                        public void onNext(FlowServiceStatus value) {}

                        @Override
                        public void onError(Throwable t) {}

                        @Override
                        public void onCompleted() {}
                      });

                  OutputInstructionProto outputInstructionProto =
                      OutputInstructionProto.newBuilder()
                          .setPort(
                              PortProtoOuterClass.PortProto.newBuilder()
                                  .setPortNumber(firstEdge.getSrcPort())
                                  .build())
                          .build();

                  instructionProto =
                      InstructionProto.newBuilder().setOutput(outputInstructionProto).build();

                  trafficTreatmentProto =
                      TrafficTreatmentProto.newBuilder()
                          .addAllInstructions(instructionProto)
                          .build();

                  OutboundPacketProto outboundPacketProto2 =
                      OutboundPacketProto.newBuilder()
                          .setDeviceId(inboundPacketProto.getConnectPoint().getDeviceId())
                          .setTreatment(trafficTreatmentProto)
                          .setData(inboundPacketProto.getData())
                          .build();

                  packetOutServiceStub.emit(
                      outboundPacketProto2,
                      new StreamObserver<PacketOutStatus>() {
                        @Override
                        public void onNext(PacketOutStatus value) {}

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
