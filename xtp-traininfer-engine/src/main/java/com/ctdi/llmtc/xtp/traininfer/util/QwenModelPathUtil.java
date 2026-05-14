package com.ctdi.llmtc.xtp.traininfer.util;

import com.ctdi.llmtc.xtp.traininfer.config.QwenModelPathConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QwenModelPathUtil implements ModelPathUtil {

    @Autowired
    private QwenModelPathConfig config;

    //base_dir = '/mnt/hpfs' if  platform.machine() == 'aarch64' else '/data'
    public String getBaseDir() {
        String machineArch = System.getProperty("os.arch");
        if ("aarch64".equals(machineArch)) {
            return config.getAarch64BaseDir();
        }else {
            return config.getOtherBaseDir();
        }
    }

    //dataset_dir =  '/mnt/hpfs/train_files/datasets' if  platform.machine() == 'aarch64' else '/dataset/data'
    public String getDataSetDir() {
        String machineArch = System.getProperty("os.arch");
        if ("aarch64".equals(machineArch)) {
            return config.getAarch64DataSetDir();
        }else {
            return config.getOtherDataSetDir();
        }
    }

    /**
     * 数据集路径
     */
    //    src_dataset_file_path = lambda fn,suffix: f'{dataset_dir}/{fn}.{suffix}'
    public String getSrcDatasetFilePath(String fn, String suffix) {
        return getDataSetDir() + "/" + fn + "." + suffix;
    }

    /**
     * 模型训练、量化、推理相关路径
     */
    //    yaml_file_path = lambda task_id,op:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_{op}_yaml/task/{task_id}
    public String getYamlFilePath(String taskId, String op) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_" + op + "_yaml/task/" + taskId;
    }

    //    train_dir_path =  lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_train_yaml/task/{task_id}'
    public String getTrainDirPath(String taskId) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_train_yaml/task/" + taskId;
    }

    public String getTplDir(String op) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_" + op + "_yaml/";
    }

    //    dpo_dir_path =  lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_dpo_yaml/task/{task_id}'
    public String getDpoDirPath(String taskId) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_dpo_yaml/task/" + taskId;
    }

    //    inference_dir_path =  lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_inference_y
    public String getInferenceDirPath(String taskId) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_inference_yaml/task/" + taskId;
    }

    public String getInferenceEvalDirPath(String taskId) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_inference_eval_yaml/task/" + taskId;
    }

    //    eval_dir_path =  lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_eval_yaml/task/{task_id}'
    public String getEvalDirPath(String taskId) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_eval_yaml/task/" + taskId;
    }

    //    quant_dir_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_quantization_yaml/task/{task_id}'
    public String getQuantDirPath(String taskId) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_quantization_yaml/task/" + taskId;
    }

    //    train_config_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_train_yaml/task/{task_id}/train_in_docker.yaml'
    public String getTrainConfigFilePath(String taskId) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_train_yaml/task/" + taskId + "/train_in_docker.yaml";
    }

    //    dpo_config_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_dpo_yaml/task/{task_id}/train_in_docker.yaml'
    public String getDpoConfigFilePath(String taskId) {
        return getBaseDir() + config.getModelInputDir() + "/examples/auto_generate_dpo_yaml/task/" + taskId + "/train_in_docker.yaml";
    }

    //datainfo_file_path = f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/data/dataset_info.json'
    public String getDatainfoFilePath() {
        return getBaseDir() + config.getModelInputDir() + "/data/dataset_info.json";
    }

    //    dst_dataset_file_path = lambda fn:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/data/{fn}.json'
    public String getDstDatasetFilePath(String fn) {
        return getBaseDir() + config.getModelInputDir() + "/data/" + fn + ".json";
    }

    /**
     * 模型输出路径：包括模型文件和各种日志
     */
    //    train_log_file_path = lambda task_id,role:f'{base_dir}/cyc_qwen2_workspace/saves/output_dir/{task_id}-{role}-log'
    public String getTrainLogFilePath(String taskId, String role) {
        return getBaseDir() + config.getModelOutputDir() + "/" + taskId + "-" + role + "-log";
    }

    //    eval_log_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/saves/output_dir/{task_id}/eval/results.log'
    public String getEvalLogFilePath(String taskId) {
        return getBaseDir() + config.getModelOutputDir() + "/" + taskId + "/eval/results.log";
    }

    //    inference_log_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/saves/output_dir/{task_id}-inference-log'
    public String getInferenceLogFilePath(String taskId) {
        return getBaseDir() + config.getModelOutputDir() + "/" + taskId + "-inference-log";
    }

    //    model_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/saves/output_dir/{task_id}'
    public String getModelFilePath(String taskId) {
        return getBaseDir() + config.getModelOutputDir() + "/" + taskId;
    }

    // intent_sync_path = lambda task_id:f'/data/nfs/lora_intent/{task_id}'
    public String getIntentSyncPath(String taskId) {
        return config.getIntentSyncDir() + "/" + taskId;
    }

    public String getTemplateDir(String op, String deployMode) {
        return config.getTemplateDir() + "/" + deployMode + "/auto_generate_" + op + "_yaml/";
    }

}
