load("@dnos_core_grpc//tools/build/bazel:generate_workspace.bzl", "COMPILE")
load("@io_bazel_rules_docker//java:image.bzl", "java_image")

COMPILE_DEPS = COMPILE + [
    "@dnos_core_grpc//protobuf/proto:onos-core-protobuf-services-proto",
    "@dnos_core_grpc//protobuf/proto:onos-protobuf-models-proto",
    "@dnos_services//config:dnos-services-config-service",
    "@com_google_protobuf//:protobuf_java",
    "@openflowj//jar",
    "@io_netty_netty//jar",
    "@com_google_guava_guava//jar",
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
