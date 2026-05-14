package com.ctdi.llmtc.xtp.traininfer.util;

import org.springframework.stereotype.Component;

@Component
public interface ModelPathUtil {

    public String getBaseDir();

    /**
     * 数据集路径
     */
    //    src_dataset_file_path = lambda fn,suffix: f'{dataset_dir}/{fn}.{suffix}'
    public String getSrcDatasetFilePath(String fn, String suffix);

    /**
     * 模型训练、量化、推理相关路径
     */
    //    yaml_file_path = lambda task_id,op:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_{op}_yaml/task/{task_id}/k8
    public String getYamlFilePath(String taskId, String op);
    //    train_dir_path =  lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_train_yaml/task/{task_id}'
    public String getTrainDirPath(String taskId);

    public String getTplDir(String op);

    //    dpo_dir_path =  lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_dpo_yaml/task/{task_id}'
    public String getDpoDirPath(String taskId);
    //    inference_dir_path =  lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_inference_yaml/task/{task_id}'
    public String getInferenceDirPath(String taskId);
    //    inference_eval_dir_path =  lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_inference_eval_yaml/task/{task_id}'
    public String getInferenceEvalDirPath(String taskId);
    //    eval_dir_path =  lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_eval_yaml/task/{task_id}'
    public String getEvalDirPath(String taskId);
    //    quant_dir_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_quantization_yaml/task/{task_id}'
    public String getQuantDirPath(String taskId);
    //    dst_dataset_file_path = lambda fn:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/data/{fn}.json'
    public String getDstDatasetFilePath(String fn);
    //    train_config_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_train_yaml/task/{task_id}/train_in_docker.yaml'
    public String getTrainConfigFilePath(String taskId);
    //    dpo_config_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples/auto_generate_dpo_yaml/task/{task_id}/train_in_docker.yaml'
    public String getDpoConfigFilePath(String taskId);
    //datainfo_file_path = f'{base_dir}/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/data/dataset_info.json'
    public String getDatainfoFilePath();

    /**
     * 模型输出路径：包括模型文件和各种日志
     */
    //    train_log_file_path = lambda task_id,role:f'{base_dir}/cyc_qwen2_workspace/saves/output_dir/{task_id}-{role}-log'
    public String getTrainLogFilePath(String taskId, String role);
    //    eval_log_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/saves/output_dir/{task_id}/eval/results.log'
    public String getEvalLogFilePath(String taskId);
    //    inference_log_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/saves/output_dir/{task_id}-inference-log'
    public String getInferenceLogFilePath(String taskId);
    //    model_file_path = lambda task_id:f'{base_dir}/cyc_qwen2_workspace/saves/output_dir/{task_id}'
    public String getModelFilePath(String taskId);

    public String getIntentSyncPath(String taskId);

    public String getTemplateDir(String op, String deployMode);
}
