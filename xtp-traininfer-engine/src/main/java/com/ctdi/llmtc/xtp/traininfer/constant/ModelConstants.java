package com.ctdi.llmtc.xtp.traininfer.constant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yangla
 * @since 2025/6/11
 */
public class ModelConstants {

    // 公共常量---------------------------
    public static final List<String> SYSTEM_DATASET = List.of(
            "identity",
            "alpaca_zh_demo",
            "dpo_zh_demo"
    );
    public static final String ASCEND_RESOURCE_NAME = "huawei.com/Ascend910";
    public static final int POD_RESTART_LIMIT = 8;
    public static final String POD_STATUS_PENDING = "Pending";
    public static final String POD_STATUS_RUNNING = "Running";
    public static final String POD_STATUS_SUCCEEDED = "Succeeded";
    public static final String POD_STATUS_FAILED = "Failed";
    public static final String POD_STATUS_UNKNOWN = "Unknown";
    public static final String INFER_DEPLOY_PREFIX = "qwen2-vllm-deployment-";
    public static final String NS_TRAIN = "qwen2-train";
    public static final String NS_INFERENCE = "qwen2-inference";
    public static final String NS_EVAL = "qwen2-eval";
    public static final String OP_DPO = "dpo";
    public static final String OP_TRAIN = "train";
    public static final String OP_EVAL = "eval";
    public static final String OP_INFER= "inference";
    public static final String OP_INFER_EVAL= "inference_eval";
    public static final boolean IS_VOLCANO = false;
    public static final String X86_ARCH = "x86";
    public static final String ARM_ARCH = "arm";

    // k8s相关常量-----------------------
    public static final String K8S_CONFIG_FILE = "k8s_config.yaml";
    public static final String TRAIN_PARAM_CONFIG_FILE = "train_in_docker.yaml";
    public static final String EVAL_PARAM_CONFIG_FILE = "eval_in_docker.yaml";
    public static final String INFER_PARAM_CONFIG_FILE = "inference_in_docker.yaml";
    public static final String DOCKER_COMPOSE_CONFIG_FILE = "docker_compose_config.yaml";

    // 模型名称相关常量----------------------
    // Qwen 系列模型
    public static final String QWEN_1_5_14B = "qwen1.5_14b";
    public static final String QWEN_2_1_5B = "qwen2_1.5b";
    public static final String QWEN_2_7B = "qwen2_7b";
    public static final String QWEN_2_5_14B = "qwen2.5_14b";
    public static final String QWEN_2_72B = "qwen2_72b";
    public static final String QWEN_2_5_72B_INSTRUCT = "qwen2.5-72b-instruct";
    public static final String QWEN3_32B = "qwen3-32B";

    // 训练相关的常量---------------------------
    public static final String TRAIN_METHOD_LORA = "lora";
    public static final String TRAIN_METHOD_QLORA = "qlora";
    public static final String TRAIN_METHOD_FULL = "full";
    public static final String TRAIN_METHOD_DPO = "dpo";
    public static final String TRAIN_DISTRIBUTED_K8S_TPL = "distributed";
    public static final String TRAIN_OTHER_K8S_TPL = "other";

    public static final String SAVE_DIR = "/app/saves/output_dir/";

    public static final String EVAL_8GPU_K8S_TPL = "8gpu";
    public static final String EVAL_OTHER_K8S_TPL = "other";

    public static final String METHOD_USER_LORA = "use_lora";
    public static final String METHOD_OTHER = "other";

    // 标签相关常量---------------------------
    public static final String PS_LABEL_KEY = "project-space";
    public static final String TRAIN_LABEL_KEY = "train";
    public static final String EVAL_LABEL_KEY = "eval";
    public static final String INFER_LABEL_KEY = "inferencewx";
    public static final String PS_ID_PUBLIC_TRAIN = "public-train";
    public static final String PS_ID_PUBLIC_INFER = "public-inference";

    // docker compose 部署相关常量-----------------------
    public static final String DOCKER_TPL_CONF = "ascend_docker_template.yaml";
    public static final String DOCKER_PARAM_TPL_CONF = "param_template.yaml";
    public static final String DOCKER_PARAM_LORA_TPL_CONF = "parm_lora_template.yaml";

    // 项目空间中推理任务优先级----------------------------
    public static final String PRIORITY_CLASS_LOW = "infer-priority-low";
    public static final String PRIORITY_CLASS_MEDIUM = "infer-priority-medium";
    public static final String PRIORITY_CLASS_HIGH = "infer-priority-high";
    public static final String PRIORITY_CLASS_CRITICA = "infer-priority-critica";

    public static final Map<Integer, String> DEEPSPEED_CONF = new HashMap<>();

    public static final Map<String, String> DEEPSPEED_CONF2 = new HashMap<>();

    public static final Map<String, String> MODEL_CONF = new HashMap<>();

    public static final Map<String, String> TRAIN_K8S_TPL_CONF = new HashMap<>();

    public static final Map<String, String> EVAL_K8S_TPL_CONF = new HashMap<>();

    public static final Map<String, String> INFER_K8S_TPL_CONF = new HashMap<>();

    public static final Map<String, String> TRAIN_PARAM_TPL_CONF = new HashMap<>();

    public static final Map<String, String> EVAL_PARAM_TPL_CONF = new HashMap<>();

    public static final Map<String, String> INFER_PARAM_TPL_CONF = new HashMap<>();

    public static final Map<String, Integer> MODEL_NEED_CARD_CONF = new HashMap<>();

    public static final Map<String, String> PS_INFER_PRIORITY_CONF = new HashMap<>();

    public static final Map<String, String> CLUSTER_TYPE = new HashMap<>();

    static {
        DEEPSPEED_CONF.put(1, "/app/examples/deepspeed/ds_z1_config.json");
        DEEPSPEED_CONF.put(2, "/app/examples/deepspeed/ds_z2_config.json");
        DEEPSPEED_CONF.put(3, "/app/examples/deepspeed/ds_z3_config.json");

        DEEPSPEED_CONF2.put(QWEN_1_5_14B, "/app/examples/deepspeed/ds_z3_config.json");
        DEEPSPEED_CONF2.put(QWEN_2_7B, "/app/examples/deepspeed/ds_z2_config.json");
        DEEPSPEED_CONF2.put(QWEN_2_1_5B, "/app/examples/deepspeed/ds_z2_config.json");
        DEEPSPEED_CONF2.put(QWEN_2_72B, "/app/examples/deepspeed/ds_z3_config.json");

        MODEL_CONF.put(QWEN_1_5_14B, "/llm_models/Qwen1.5-14B-Chat");
        MODEL_CONF.put(QWEN_2_1_5B, "/llm_models/Qwen2-1.5B-Instruct");
        MODEL_CONF.put(QWEN_2_7B, "/llm_models/Qwen2-7B-Instruct");
        MODEL_CONF.put(QWEN_2_5_14B, "/llm_models/Qwen2.5-14B-Instruct");
        MODEL_CONF.put(QWEN_2_72B, "/llm_models/Qwen2-72B-Instruct");
        MODEL_CONF.put(QWEN_2_5_72B_INSTRUCT, "/llm_models/Qwen2.5-72B-Instruct");
        MODEL_CONF.put(QWEN3_32B, "/llm_models/Qwen3-32B");

        TRAIN_K8S_TPL_CONF.put(TRAIN_DISTRIBUTED_K8S_TPL, "ascend_k8s_template_distributed.yaml");
        TRAIN_K8S_TPL_CONF.put(TRAIN_OTHER_K8S_TPL, "ascend_k8s_template.yaml");

        EVAL_K8S_TPL_CONF.put(EVAL_8GPU_K8S_TPL, "ascend_k8s_config_8gpu_template.yaml");
        EVAL_K8S_TPL_CONF.put(EVAL_OTHER_K8S_TPL, "ascend_k8s_config_template.yaml");

        INFER_K8S_TPL_CONF.put(QWEN_1_5_14B, "ascend_k8s_config_8gpu_template_2wx.yaml");
        INFER_K8S_TPL_CONF.put(QWEN_2_1_5B, "ascend_k8s_config_template.yaml");
        INFER_K8S_TPL_CONF.put(QWEN_2_7B, "ascend_k8s_config_template.yaml");
        INFER_K8S_TPL_CONF.put(QWEN_2_72B, "ascend_k8s_config_8gpu_template_4wx.yaml");
        INFER_K8S_TPL_CONF.put(QWEN_2_5_72B_INSTRUCT, "ascend_k8s_config_8gpu_template_4wx.yaml");
        INFER_K8S_TPL_CONF.put(QWEN3_32B, "ascend_k8s_config_8gpu_template_2wx.yaml");

        TRAIN_PARAM_TPL_CONF.put(TRAIN_METHOD_LORA, "lora_sft_ds_template.yaml");
        TRAIN_PARAM_TPL_CONF.put(TRAIN_METHOD_QLORA, "qlora_sft_ds_template.yaml");
        TRAIN_PARAM_TPL_CONF.put(TRAIN_METHOD_FULL, "full_sft_ds_template.yaml");
        TRAIN_PARAM_TPL_CONF.put(TRAIN_METHOD_DPO, "lora_dpo_ds_template.yaml");

        EVAL_PARAM_TPL_CONF.put(METHOD_USER_LORA, "template_lora.yaml");
        EVAL_PARAM_TPL_CONF.put(METHOD_OTHER, "template.yaml");

        INFER_PARAM_TPL_CONF.put(METHOD_USER_LORA, "ascend_template_lora.yaml");
        INFER_PARAM_TPL_CONF.put(METHOD_OTHER, "ascend_template.yaml");

        MODEL_NEED_CARD_CONF.put(QWEN_1_5_14B, 1);
        MODEL_NEED_CARD_CONF.put(QWEN_2_1_5B, 1);
        MODEL_NEED_CARD_CONF.put(QWEN_2_7B, 1);
        MODEL_NEED_CARD_CONF.put(QWEN_2_72B, 4);
        MODEL_NEED_CARD_CONF.put(QWEN_2_5_72B_INSTRUCT, 4);
        MODEL_NEED_CARD_CONF.put(QWEN3_32B, 1); // 暂时没有用

        PS_INFER_PRIORITY_CONF.put(QWEN_1_5_14B, PRIORITY_CLASS_LOW);
        PS_INFER_PRIORITY_CONF.put(QWEN_2_1_5B, PRIORITY_CLASS_LOW);
        PS_INFER_PRIORITY_CONF.put(QWEN_2_7B, PRIORITY_CLASS_LOW);
        PS_INFER_PRIORITY_CONF.put(QWEN_2_72B, PRIORITY_CLASS_HIGH);
        PS_INFER_PRIORITY_CONF.put(QWEN_2_5_72B_INSTRUCT, PRIORITY_CLASS_HIGH);
        PS_INFER_PRIORITY_CONF.put(QWEN3_32B, PRIORITY_CLASS_CRITICA); // 暂时没有用

        CLUSTER_TYPE.put("GZ", "Ascend910");
        CLUSTER_TYPE.put("QD", "NVIDIA");

    }

}
