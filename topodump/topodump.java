package topodump;

import api.topostore.TopoEdge;
import api.topostore.TopoEdgeType;
import api.topostore.TopoSwitch;
import config.ConfigService;
import drivers.controller.Controller;
import drivers.onos.OnosController;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onosproject.grpc.net.models.EventNotificationGrpc;
import org.onosproject.grpc.net.models.ServicesProto;

import java.util.Set;

public class topodump {
  private static Logger log = Logger.getLogger(topodump.class);


    static String serverId = null;
    static String clientId = "linkfailuredetector2";


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
      Controller finalController = controller;
      Set<TopoSwitch> topoSwitches = finalController.topoStore.getSwitches();

      Set<TopoEdge> topoEdges = finalController.topoStore.getTopoEdges();
      EventNotificationGrpc.EventNotificationStub linkEventNotification;

      channel =
              ManagedChannelBuilder.forAddress(controllerIP, Integer.parseInt(grpcPort))
                      .usePlaintext()
                      .build();

      linkEventNotification = EventNotificationGrpc.newStub(channel);

      ServicesProto.RegistrationRequest request =
              ServicesProto.RegistrationRequest.newBuilder().setClientId(clientId).build();
      linkEventNotification.register(
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
                      .setType(ServicesProto.topicType.LINK_EVENT)
                      .build();

      class Event implements Runnable {

          @Override
          public void run() {

              linkEventNotification.onEvent(
                      topic,
                      new StreamObserver<ServicesProto.Notification>() {
                          @Override
                          public void onNext(ServicesProto.Notification value) {

                              TopoEdge edge = new TopoEdge();
                              edge.setSrc(value.getLinkEvent().getLink().getSrc().getDeviceId());
                              edge.setSrcPort(value.getLinkEvent().getLink().getSrc().getPortNumber());
                              edge.setDst(value.getLinkEvent().getLink().getDst().getDeviceId());
                              edge.setDstPort(value.getLinkEvent().getLink().getDst().getPortNumber());
                              edge.setType(TopoEdgeType.SWITCH_SWITCH);

                              switch (value.getLinkEvent().getLinkEventType()) {
                                  case LINK_ADDED:
                                      //log.info("LINK_ADDED");
                                      finalController.topoStore.addEdge(edge);

                                      break;
                                  case LINK_REMOVED:
                                      //log.info("LINK_REMOVED");
                                      finalController.topoStore.removeEdge(edge);
                                      break;
                                  case LINK_UPDATED:
                                      //log.info("LINK_UPDATED");
                              }

                              log.info("Number of links:" +
                                      finalController.topoStore.getTopoEdges().size());
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
