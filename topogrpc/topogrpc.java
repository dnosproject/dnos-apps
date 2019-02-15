package topogrpc;

import config.ConfigService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onosproject.grpc.net.models.HostProtoOuterClass.HostProto;
import org.onosproject.grpc.net.topology.models.TopologyEdgeProtoOuterClass.TopologyEdgeProto;
import org.onosproject.grpc.net.topology.models.TopologyGraphProtoOuterClass.TopologyGraphProto;
import org.onosproject.grpc.net.topology.models.TopologyProtoOuterClass.TopologyProto;
import org.onosproject.grpc.grpcintegration.models.ServicesProto;
import org.onosproject.grpc.grpcintegration.models.TopoServiceGrpc;
import org.onosproject.grpc.grpcintegration.models.TopoServiceGrpc.TopoServiceStub;


public class topogrpc {
  private static Logger log = Logger.getLogger(topogrpc.class);

  public static void main(String[] args) {

    ManagedChannel channel;
    String controllerIP;
    String grpcPort;

    ConfigService configService = new ConfigService();
    configService.init();
    controllerIP = configService.getConfig().getControllerIp();
    grpcPort = configService.getConfig().getGrpcPort();
    TopoServiceStub topologyServiceStub;

    // Creates a gRPC channel
    channel =
        ManagedChannelBuilder
                .forAddress(controllerIP, Integer.parseInt(grpcPort))
                .usePlaintext()
                .build();

    topologyServiceStub = TopoServiceGrpc.newStub(channel);
    ServicesProto.Empty empty = ServicesProto.Empty.newBuilder().build();

    // Retrieves current topology information
    topologyServiceStub.currentTopology(empty,
            new StreamObserver<TopologyProto>() {
              @Override
              public void onNext(TopologyProto value) {

                  log.info("Number of links:" + value.getLinkCount());
              }

              @Override
              public void onError(Throwable t) {}

              @Override
              public void onCompleted() {}
            });

    // Retrieves topology graph
    topologyServiceStub.getGraph(empty, new StreamObserver<TopologyGraphProto>() {
        @Override
        public void onNext(TopologyGraphProto value) {

            for(TopologyEdgeProto topologyEdgeProto: value.getEdgesList()) {

                log.info(topologyEdgeProto.getLink().getSrc().getDeviceId() +
                        ":" + topologyEdgeProto.getLink().getSrc().getPortNumber() +
                " -->" + topologyEdgeProto.getLink().getDst().getDeviceId() +
                         ":" + topologyEdgeProto.getLink().getDst().getPortNumber());
            }
        }

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
    });

    // Retrieves list of hosts in the network topology
    topologyServiceStub.getHosts(empty, new StreamObserver<ServicesProto.HostsProto>() {
        @Override
        public void onNext(ServicesProto.HostsProto value) {
            for(HostProto hostProto:value.getHostList()) {

                log.info(hostProto.getIpAddresses(0)
                + ";" + hostProto.getHostId().getMac() + ";" +
                hostProto.getLocation().getConnectPoint().getDeviceId()
                + ":" + hostProto.getLocation().getConnectPoint().getPortNumber());
            }
        }

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
    });





    while(true) {
    }
  }
}
