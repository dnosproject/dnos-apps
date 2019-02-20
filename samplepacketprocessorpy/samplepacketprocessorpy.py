import grpc
import EventNotificationProto_pb2
import ServicesProto_pb2_grpc
import threading

# A Simple packet processor.
def packetprocessor():

     channel = grpc.insecure_channel('localhost:50051')
     eventNotificationStub = ServicesProto_pb2_grpc.EventNotificationStub(channel)

     request = EventNotificationProto_pb2.RegistrationRequest(clientId = "packet_processor_python")
     topic = EventNotificationProto_pb2.Topic(clientId = "packet_processor_python"
                                              , type = 0)

     # Register to PACKET_EVENT
     response = eventNotificationStub.register(request)
     eventObserver = eventNotificationStub.onEvent(topic)


     for event in eventObserver:
          print(event)
     while True:
          continue

if __name__ == '__main__':
     t = threading.Thread(target=packetprocessor)
     t.start()

