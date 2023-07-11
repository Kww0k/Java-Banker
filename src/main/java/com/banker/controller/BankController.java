package com.banker.controller;

import com.banker.entity.AddRequestDto;
import com.banker.entity.FinalDto;
import com.banker.entity.InitNumber;
import com.banker.entity.RestBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: Silvery
 * @Date: 2023/7/5 15:55
 */
@RestController
@RequestMapping("/bank")
public class BankController {

    private Integer processesNumber; // 进程数
    private Integer assetsNumber; // 资源数
    private List<Integer> assetsList; // 每个资源总数
    private List<Integer> availableList; // 每个资源剩余资源数
    private List<Integer> request;
    private int[][] need = new int[100][100];
    private int[][] max = new int[100][100];
    private int[][] allocation = new int[100][100];
    private int processNum;
    private List<Integer> chokeList = new ArrayList<>();

    @PostMapping("/initNumber")
    public RestBean initNumber(@RequestBody InitNumber initNumber) {
        processesNumber = initNumber.getProcessesNumber();
        assetsNumber = initNumber.getAssetsNumber();
        if (processesNumber != null && assetsNumber != null)
            return new RestBean<>(200, null);
        return new RestBean<>(400, "请检查参数是否正确");
    }

    @PostMapping("/initAssets")
    public RestBean initAssets(@RequestBody List<Integer> assetesList) {
        this.availableList = assetesList;
        return new RestBean<>(200, null);
    }

    @PostMapping("/initOtherAndMath")
    public RestBean initOtherAndMath(@RequestBody FinalDto finalDto) {
        for (int i = 0; i < (finalDto.getNeed().size() / assetsNumber); i++)
            for (int j = 0; j < assetsNumber; j++)
                need[i][j] = finalDto.getNeed().get(i * assetsNumber + j);
        for (int i = 0; i < (finalDto.getMax().size() / assetsNumber); i++)
            for (int j = 0; j < assetsNumber; j++)
                max[i][j] = finalDto.getMax().get(i * assetsNumber + j);
        for (int i = 0; i < (finalDto.getAllocation().size() / assetsNumber); i++)
            for (int j = 0; j < assetsNumber; j++)
                allocation[i][j] = finalDto.getAllocation().get(i * assetsNumber + j);
        assetsList = new ArrayList<>();
        for (int i = 0; i < availableList.size(); i ++) {
            int sum = 0;
            for (int[] arr : allocation)
                sum += arr[i];
            assetsList.add(sum);
        }

        return safetyCheck(null, null);
    }

    @PostMapping("/addRequest")
    public RestBean addRequest(@RequestBody AddRequestDto addRequestDto) {
        // 1.获取需要的资源
        request = addRequestDto.getRequestList();
        processNum = addRequestDto.getProcessNum();

        for (int i = 0; i< request.size(); i++) {
            // 2.判断有没有超过need
            if (request.get(i) > need[processNum][i])
                return new RestBean<>(400, "对不起，系统出错");

            // 3.判断剩余资源是否充足 第一种阻塞情况
            if (request.get(i) > availableList.get(i))
                return new RestBean<>(400, "剩余资源不足会引起进程" + (processNum + 1) + "处于阻塞状态");
        }

        // 4.尝试分配资源
        for (int i = 0; i< request.size(); i++) {
            need[processNum][i] = need[processNum][i] - request.get(i);
            allocation[processNum][i] = allocation[processNum][i] + request.get(i);
            availableList.set(i, availableList.get(i) - request.get(i));
        }

        // 执行安全算法
        return safetyCheck(processNum, request); //传入进程号和request
    }

    @PostMapping("/deleteRequest")
    public RestBean deleteRequest(@RequestBody AddRequestDto addRequestDto) {
        return null;
    }

    @PostMapping("/addPro")
    public RestBean addPro(@RequestBody AddRequestDto addRequestDto) {
        processNum = addRequestDto.getProcessNum();
        // 检查进程数是否已达到最大值
        if (processesNumber >= 100)
            return new RestBean(400, "添加失败，进程数已达到最大值");

        // 检查资源请求列表是否符合需求
        List<Integer> requestList = addRequestDto.getRequestList();
        for (int i = 0; i < assetsNumber; i++) {
            if (requestList.get(i) > assetsList.get(i))
                return new RestBean(400, "添加失败，资源请求数超过资源总数");
            if (requestList.get(i) > availableList.get(i))
                return new RestBean(400, "添加失败，资源请求数超过当前可用资源数");
        }

        // 创建新线程
        for (int i = 0; i < assetsNumber; i++) {
            max[processNum][i] = requestList.get(i);
            allocation[processNum][i] = 0;
            need[processNum][i] = requestList.get(i);
            availableList.set(i, availableList.get(i) - requestList.get(i));
        }
        processesNumber++;
        return new RestBean(200, "添加成功");
    }

    public RestBean safetyCheck(Integer process, List<Integer> request) {

        // 检查是否存在超出资源数量的最大需求
        for (int i = 0; i < processesNumber; i++)
            for (int j = 0; j < assetsNumber; j++)
                if (max[i][j] > assetsList.get(j))
                    return new RestBean<>(400, "进程 " + (i + 1) + " 的最大需求超过了资源 " + (j + 1) + " 的总数");

        // 检查是否存在超出最大需求的分配情况
        for (int i = 0; i < processesNumber; i++)
            for (int j = 0; j < assetsNumber; j++)
                if (allocation[i][j] > max[i][j])
                    return new RestBean<>(400, "进程 " + (i + 1) + " 的资源 " + (j + 1) + " 分配超过了最大需求");


        // 检查是否存在超出资源数量的需求和分配情况
        for (int i = 0; i < processesNumber; i++)
            for (int j = 0; j < assetsNumber; j++)
                if (need[i][j] > assetsList.get(j) || allocation[i][j] > assetsList.get(j))
                    return new RestBean<>(400, "进程 " + (i + 1) + " 的需求或分配超过了资源 " + (j + 1) + " 的总数");


        // 检查是否所有资源都已经初始化
        if (assetsList.contains(null))
            return new RestBean<>(400, "请初始化所有资源");

        // 检查是否存在阻塞的进程
        for(int i = 0; i < processesNumber; i++) {
            // 检查进程是否满足需求
            boolean isSafe = true;
            for (int j = 0; j < assetsNumber; j++) {
                if (need[i][j] > assetsList.get(j)) {
                    isSafe = false;
                    break;
                }
            }
            if (!isSafe)
                return new RestBean<>(400, "进程 " + (i + 1) + " 阻塞");
        }

        int[] work = new int[assetsNumber];
        boolean[] finish = new boolean[processesNumber];
        List<Integer> safeSeq = new ArrayList<>();

        // 初始化work为available
        for (int i = 0; i < assetsNumber; i++) {
            work[i] = availableList.get(i);
        }

        // 初始化完成状态
        for (int i = 0; i < processesNumber; i++)
            finish[i] = false;

        int count = 0; // 计数器

        // 遍历所有进程
        while (count < processesNumber) {
            boolean found = false;
            for (int i = 0; i < processesNumber; i++) {
                if (!finish[i]) {
                    boolean canExecute = true;
                    for (int j = 0; j < assetsNumber; j++) {
                        if (need[i][j] > work[j]) {
                            canExecute = false;
                            break;
                        }
                    }
                    if (canExecute) {
                        for (int j = 0; j < assetsNumber; j++) {
                            work[j] += allocation[i][j];
                        }
                        safeSeq.add(i);
                        finish[i] = true;
                        found = true;
                        count++;
                    }
                }
            }

            // 没有找到可以执行的进程，说明系统处于不安全状态
            if (!found) {
                safeSeq.clear();
                break;
            }
        }
        if (safeSeq.isEmpty()) {
            if (process == null) {
                return new RestBean<>(201, "系统处于不安全状态");
            }
            else {
                // 7. 不安全，则阻塞，第二种阻塞情况
                // 回滚数据
                for (int i = 0; i< request.size(); i++) {
                    need[processNum][i] = need[processNum][i] + request.get(i);
                    allocation[processNum][i] = allocation[processNum][i] - request.get(i);
                    availableList.set(i, availableList.get(i) + request.get(i));
                }
                String str = "进程" + (process + 1) + "处于阻塞状态";
                return new RestBean<>(400, "系统处于不安全状态," + str);
            }
        } else {
            // 6. 安全， 正式分配
            return new RestBean<>(200, safeSeq);
        }
    }
}
