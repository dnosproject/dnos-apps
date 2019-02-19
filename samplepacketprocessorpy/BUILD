load("@io_bazel_rules_docker//python:image.bzl", "py_image")

COMPILE_DEPS = [
    "@dnos_core_grpc_python//protobuf/proto:onos-core-protobuf-services-proto",
]

py_binary(
    name = "samplepacketprocessorpy",
    srcs = ["samplepacketprocessorpy.py"],
    main = "samplepacketprocessorpy.py",
    deps = COMPILE_DEPS,
)
