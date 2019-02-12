package topogrpc;

import config.ConfigService;
import drivers.controller.Controller;
import drivers.onos.OnosController;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;

import org.onosproject.grpc.net.models.ServicesProto;
import org.onosproject.grpc.net.models.TopoServiceGrpc;
import org.onosproject.grpc.net.topology.models.TopologyEdgeProtoOuterClass.TopologyEdgeProto;
import org.onosproject.grpc.net.topology.models.TopologyGraphProtoOuterClass.TopologyGraphProto;
import org.onosproject.grpc.net.topology.models.TopologyProtoOuterClass.TopologyProto;


public class topogrpc {
  private static Logger log = Logger.getLogger(topogrpc.class);



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
    TopoServiceGrpc.TopoServiceStub topologyServiceStub;

    channel =
        ManagedChannelBuilder.forAddress(controllerIP, Integer.parseInt(grpcPort))
            .usePlaintext()
            .build();


    topologyServiceStub = TopoServiceGrpc.newStub(channel);
    ServicesProto.Empty empty = ServicesProto.Empty.newBuilder().build();

    topologyServiceStub.currentTopology(empty,
            new StreamObserver<TopologyProto>() {
              @Override
              public void onNext(TopologyProto value) {

                  log.info("Link count:" + value.getLinkCount());
              }

              @Override
              public void onError(Throwable t) {

              }

              @Override
              public void onCompleted() {

              }
            });


    topologyServiceStub.getGraph(empty, new StreamObserver<TopologyGraphProto>() {
        @Override
        public void onNext(TopologyGraphProto value) {
            for(TopologyEdgeProto topologyEdgeProto: value.getEdgesList()) {

                log.info(topologyEdgeProto.getLink().getSrc().getDeviceId() +
                ":" + topologyEdgeProto.getLink().getDst().getDeviceId());

            }
        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onCompleted() {

        }
    });

    while(true) {

    }



  }
}
