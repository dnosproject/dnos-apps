import grpc

def run():
     channel = grpc.insecure_channel('localhost:50051')

     while True:
        print("hello")




if __name__ == '__main__':
     run()