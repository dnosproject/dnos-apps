package linkfailuredetector;

import config.ConfigService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onosproject.grpc.grpcintegration.models.ControlMessagesProto.Empty;
import org.onosproject.grpc.grpcintegration.models.EventNotificationGrpc;
import org.onosproject.grpc.grpcintegration.models.EventNotificationGrpc.EventNotificationStub;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.Notification;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.RegistrationRequest;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.RegistrationResponse;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.Topic;
import org.onosproject.grpc.grpcintegration.models.EventNotificationProto.topicType;
import org.onosproject.grpc.grpcintegration.models.TopoServiceGrpc;
import org.onosproject.grpc.net.topology.models.TopologyProtoOuterClass.TopologyProto;

public class linkfailuredetector {
  private static Logger log = Logger.getLogger(linkfailuredetector.class);

  static String serverId = null;
  static String clientId = "linkfailuredetector";

  public static void main(String[] args) {

    ManagedChannel channel;
    String controllerIP;
    String grpcPort;

    ConfigService configService = new ConfigService();
    configService.init();
    controllerIP = configService.getConfig().getControllerIp();

    grpcPort = configService.getConfig().getGrpcPort();
    EventNotificationStub linkEventNotification;
    TopoServiceGrpc.TopoServiceStub topologyServiceStub;
    Empty empty = Empty.newBuilder().build();

    channel =
        ManagedChannelBuilder.forAddress(controllerIP, Integer.parseInt(grpcPort))
            .usePlaintext()
            .build();

    linkEventNotification = EventNotificationGrpc.newStub(channel);
    topologyServiceStub = TopoServiceGrpc.newStub(channel);

    RegistrationRequest request =
        RegistrationRequest.newBuilder()
                .setClientId(clientId)
                .build();
    linkEventNotification.register(
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

    Topic topic =
        Topic.newBuilder()
            .setClientId(clientId)
            .setType(topicType.LINK_EVENT)
            .build();

    class Event implements Runnable {

      @Override
      public void run() {

        linkEventNotification.onEvent(
            topic,
            new StreamObserver<Notification>() {
              @Override
              public void onNext(Notification value) {

                switch (value.getLinkEvent().getLinkEventType()) {
                  case LINK_ADDED:
                    log.info("LINK_ADDED Event");
                      topologyServiceStub.currentTopology(empty,
                            new StreamObserver<TopologyProto>() {
                        @Override
                        public void onNext(TopologyProto value) {
                            log.info("Number of Links:" + value.getLinkCount());
                        }

                        @Override
                        public void onError(Throwable t) {}

                        @Override
                        public void onCompleted() {}
                    });


                    break;
                  case LINK_REMOVED:
                    log.info("LINK_REMOVED Event");
                      topologyServiceStub.currentTopology(empty,
                            new StreamObserver<TopologyProto>() {
                        @Override
                        public void onNext(TopologyProto value) {
                            log.info("Number of Links:" + value.getLinkCount());
                        }

                        @Override
                        public void onError(Throwable t) {}

                        @Override
                        public void onCompleted() {}
                    });

                    break;
                  case LINK_UPDATED:
                    //log.info("LINK_UPDATED");
                    break;
                }

              }

              @Override
              public void onError(Throwable t) {}

              @Override
              public void onCompleted() {}
            });

        while (true) {}
      }
    }

    Event event = new Event();
    Thread t = new Thread(event);
    t.start();
  }
}
