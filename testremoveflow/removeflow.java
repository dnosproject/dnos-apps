package testremoveflow;

import api.topostore.TopoSwitch;
import config.ConfigService;
import drivers.controller.Controller;
import drivers.onos.OnosController;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onlab.packet.Ethernet;
import org.onosproject.grpc.grpcintegration.models.FlowServiceGrpc;
import org.onosproject.grpc.grpcintegration.models.StatusProto.FlowServiceStatus;
import org.onosproject.grpc.net.flow.criteria.models.CriterionProtoOuterClass;
import org.onosproject.grpc.net.flow.criteria.models.CriterionProtoOuterClass.CriterionProto;
import org.onosproject.grpc.net.flow.criteria.models.CriterionProtoOuterClass.EthTypeCriterionProto;
import org.onosproject.grpc.net.flow.instructions.models.InstructionProtoOuterClass.InstructionProto;
import org.onosproject.grpc.net.flow.instructions.models.InstructionProtoOuterClass.OutputInstructionProto;
import org.onosproject.grpc.net.flow.models.FlowRuleProto;
import org.onosproject.grpc.net.flow.models.TrafficSelectorProtoOuterClass.TrafficSelectorProto;
import org.onosproject.grpc.net.flow.models.TrafficTreatmentProtoOuterClass.TrafficTreatmentProto;
import org.onosproject.grpc.net.models.PortProtoOuterClass;

import java.util.Set;

public class removeflow {
  private static Logger log = Logger.getLogger(removeflow.class);
  private static int TABLE_ID_CTRL_PACKETS = 0;
  private static int CTRL_PACKET_PRIORITY = 100;

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
    FlowServiceGrpc.FlowServiceStub flowServiceStub;

    channel =
        ManagedChannelBuilder.forAddress(controllerIP, Integer.parseInt(grpcPort))
            .usePlaintext()
            .build();

    flowServiceStub = FlowServiceGrpc.newStub(channel);

    Controller finalController = controller;
    Set<TopoSwitch> topoSwitches = finalController.topoStore.getSwitches();

    for (TopoSwitch topoSwitch : topoSwitches) {

      EthTypeCriterionProto ethTypeCriterionProto =
          EthTypeCriterionProto.newBuilder().setEthType(Ethernet.TYPE_IPV4).build();

      CriterionProto criterionProto =
          CriterionProto.newBuilder()
              .setEthTypeCriterion(ethTypeCriterionProto)
              .setType(CriterionProtoOuterClass.TypeProto.ETH_TYPE)
              .build();

      TrafficSelectorProto trafficSelectorProto =
          TrafficSelectorProto.newBuilder().addCriterion(criterionProto).build();

      InstructionProto instructionProto =
          InstructionProto.newBuilder()
              .setOutput(
                  OutputInstructionProto.newBuilder()
                      .setPort(
                          PortProtoOuterClass.PortProto.newBuilder()
                              .setPortNumber(CONTROLLER_PORT)
                              .build())
                      .build())
              .build();

      TrafficTreatmentProto trafficTreatmentProto =
          TrafficTreatmentProto.newBuilder().addAllInstructions(instructionProto).build();

      FlowRuleProto flowRuleProto =
          FlowRuleProto.newBuilder()
              .setTreatment(trafficTreatmentProto)
              .setSelector(trafficSelectorProto)
              .setPriority(CTRL_PACKET_PRIORITY)
              .setDeviceId(topoSwitch.getSwitchID())
              .setTableId(TABLE_ID_CTRL_PACKETS)
              .setTimeout(0)
              .setPermanent(true)
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

      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      flowServiceStub.removeFlow(
          flowRuleProto,
          new StreamObserver<FlowServiceStatus>() {
            @Override
            public void onNext(FlowServiceStatus value) {}

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
          });
    }

    while (true) {}
  }
}
