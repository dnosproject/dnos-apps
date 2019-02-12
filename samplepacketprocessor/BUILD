load("@dnos_core_grpc//tools/build/bazel:generate_workspace.bzl", "COMPILE")
load("@io_bazel_rules_docker//java:image.bzl", "java_image")
load("@io_bazel_rules_docker//container:container.bzl", "container_image")

COMPILE_DEPS = COMPILE + [
    "@dnos_core_grpc//protobuf/proto:onos-core-protobuf-services-proto",
    "@dnos_core_grpc//protobuf/proto:onos-protobuf-models-proto",
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

#java_binary(
#    name = "samplepacketprocessor_deploy",
#    srcs = glob(["*.java"]),
#    main_class = "samplepacketprocessor.samplepacketprocessor",
#    deps = COMPILE_DEPS,
#)

java_image(
    name = "samplepacketprocessorgrpc_image",
    srcs = glob(["*.java"]),
    data = [
        ":BUILD",
        "//resources:config.properties",
        "//resources:log4j.properties",
    ],
    main_class = "samplepacketprocessor.samplepacketprocessor",
    deps = COMPILE_DEPS,
)
