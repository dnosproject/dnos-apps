load("@io_bazel_rules_docker//python:image.bzl", "py_image")

COMPILE_DEPS = [
    "@dnos_core_grpc_python//protobuf/proto:onos-core-protobuf-services-proto",
    "@dnos_core_grpc_python//protobuf/proto:onos-protobuf-models-proto",
]

py_binary(
    name = "samplepacketprocessor_py",
    srcs = ["samplepacketprocessor.py"],
    main = "samplepacketprocessor.py",
    deps = COMPILE_DEPS,
)
