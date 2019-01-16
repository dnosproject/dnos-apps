load("@dnos_core_grpc//tools/build/bazel:generate_workspace.bzl", "COMPILE")
load("@io_bazel_rules_docker//java:image.bzl", "java_image")
load("@io_bazel_rules_docker//container:container.bzl", "container_image")

COMPILE_DEPS = COMPILE + [
    "@dnos_core_grpc//protobuf/proto:onos-core-protobuf-services-proto",
    "@dnos_core_grpc//protobuf/proto:onos-protobuf-models-proto",
    "@dnos_core_grpc//protobuf/proto:OutboundPacket_proto",
    "@dnos_core//api/eventServiceApi:dnos-event-service-api",
    "@dnos_core//tools/utility:dnos-utility-rest-tools",
    "@dnos_services//api/flowservice:dnos-flow-service-api",
    "@dnos_services//api/topostore:dnos-topostore-api",
    "@dnos_services//drivers/onos/topostore/impl:dnos-onos-driver-topostore",
    "@dnos_services//drivers/onos/flowservice/impl:dnos-onos-driver-flowservice",
    "@dnos_services//drivers/controller:dnos-driver-controller",
    "@dnos_services//drivers/onos:dnos-onos-driver-controller",
    "@dnos_services//config:dnos-services-config-service",
    "@com_google_protobuf//:protobuf_java",
    "@com_googlecode_json_simple//jar",
    "@onos_openflow//jar",
    "@io_netty//jar",
    "@guava//jar",
    "@kafka_clients//jar",
    "@httpcomponents_httpcore//jar",
    "@onlab_misc//jar",
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//stub",
    "@io_grpc_grpc_java//netty",
]

java_binary(
    name = "fwd_deploy",
    srcs = glob(["*.java"]),
    main_class = "fwd.fwd",
    deps = COMPILE_DEPS,
)

java_image(
    name = "fwd_image",
    srcs = glob(["*.java"]),
    data = [
        ":BUILD",
        "//resources:config.properties",
        "//resources:log4j.properties",
    ],
    main_class = "fwd.fwd",
    deps = COMPILE_DEPS,
)
