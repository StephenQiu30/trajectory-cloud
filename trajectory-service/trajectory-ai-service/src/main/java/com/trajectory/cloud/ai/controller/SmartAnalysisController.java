package com.trajectory.cloud.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trajectory.cloud.ai.factory.AiClientFactory;
import com.trajectory.cloud.ai.model.entity.Chart;
import com.trajectory.cloud.ai.service.AiAssistant;
import com.trajectory.cloud.ai.service.ChartService;
import com.trajectory.cloud.api.ai.model.dto.AiChatRequest;
import com.trajectory.cloud.api.ai.model.dto.ChartAnalysisMessage;
import com.trajectory.cloud.api.ai.model.dto.ChartGenRequest;
import com.trajectory.cloud.api.ai.model.dto.ChartQueryRequest;
import com.trajectory.cloud.api.ai.model.enums.AiModelTypeEnum;
import com.trajectory.cloud.api.ai.model.enums.ChartStatusEnum;
import com.trajectory.cloud.api.ai.model.enums.ChartTypeEnum;
import com.trajectory.cloud.api.ai.model.vo.ChartVO;
import com.trajectory.cloud.common.auth.utils.SecurityUtils;
import com.trajectory.cloud.common.cache.model.TimeModel;
import com.trajectory.cloud.common.cache.utils.ratelimit.RateLimitUtils;
import com.trajectory.cloud.common.common.*;
import com.trajectory.cloud.common.exception.BusinessException;
import com.trajectory.cloud.common.log.annotation.OperationLog;
import com.trajectory.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.trajectory.cloud.common.rabbitmq.utils.MqSender;
import com.trajectory.cloud.common.utils.ExcelUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 智能分析接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/analysis")
@Slf4j
@Tag(name = "SmartAnalysisController", description = "智能分析管理")
public class SmartAnalysisController {

    @Resource
    private ChartService chartService;

    @Resource
    private AiClientFactory aiClientFactory;

    @Resource
    private MqSender mqSender;

    @Resource
    private RateLimitUtils rateLimitUtils;

    /**
     * 智能分析 (同步)
     *
     * @param multipartFile   文件
     * @param chartGenRequest 请求
     * @return 结果
     */
    @PostMapping(value = "/gen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog(module = "BI 管理", action = "智能数据分析 (同步)")
    @Operation(summary = "智能分析 (同步)", description = "上传 Excel 进行智能分析并返回结果")
    public BaseResponse<Chart> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
            ChartGenRequest chartGenRequest) {
        log.info("智能分析 (同步) 请求: {}, 文件: {}", chartGenRequest, multipartFile.getOriginalFilename());
        String name = chartGenRequest.getName();
        String goal = chartGenRequest.getGoal();
        String chartType = chartGenRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 图表类型校验
        if (StringUtils.isNotBlank(chartType)) {
            ThrowUtils.throwIf(ChartTypeEnum.getEnumByText(chartType) == null, ErrorCode.PARAMS_ERROR,
                    "不支持的图表类型：" + chartType);
        }

        // 文件校验
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        ThrowUtils.throwIf(
                originalFilename != null && !originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"),
                ErrorCode.PARAMS_ERROR, "非法文件后缀");

        Long userId = SecurityUtils.getLoginUserId();
        // 限流
        rateLimitUtils.doRateLimit("ai:analysis:" + userId, new TimeModel(1L, TimeUnit.MINUTES), 5L, 1L);

        // 读取 Excel 转为 CSV
        String csvData;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ExcelUtils.excelToCsv(multipartFile.getInputStream(), bos);
            csvData = bos.toString();
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败", e);
        }

        // 构造 Prompt
        String prompt = "你是一个数据分析师和前端 Echarts 开发专家。请根据以下分析目标和原始数据，为我生成一个合法的 Echarts 配置。\n" +
                "分析目标：" + goal + "\n" +
                "图表名称：" + name + "\n" +
                "图表类型：" + chartType + "\n" +
                "原始数据：" + csvData + "\n" +
                "要求：\n" +
                "1. 仅返回 Echarts Option 配置对应的 JSON。\n" +
                "2. 同时给出一段不少于 100 字的数据分析结论。\n" +
                "3. 严格遵循以下输出格式，使用五个感叹号作为分隔：\n" +
                "!!!!!\n" +
                "{Echarts Option JSON}\n" +
                "!!!!!\n" +
                "{分析结论}";

        // 调用 AI
        AiChatRequest aiRequest = AiChatRequest.builder()
                .modelType(AiModelTypeEnum.DASHSCOPE.getValue())
                .message(prompt)
                .build();
        ChatLanguageModel chatModel = aiClientFactory.getChatModel(aiRequest);
        AiAssistant assistant = AiServices.builder(AiAssistant.class)
                .chatLanguageModel(chatModel)
                .build();

        String result = assistant.chat(prompt).content();
        String[] splits = result.split("!!!!!");
        if (splits.length < 3) {
            log.error("AI 生成格式错误: {}", result);
            throw new RuntimeException("AI 生成格式错误");
        }

        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        // 存库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setStatus(ChartStatusEnum.SUCCEED.getValue());
        chart.setUserId(userId);
        chartService.save(chart);

        log.info("智能分析 (同步) 处理成功, chartId: {}", chart.getId());
        return ResultUtils.success(chart);
    }

    @PostMapping(value = "/gen/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog(module = "BI 管理", action = "智能数据分析 (异步)")
    @Operation(summary = "智能分析 (异步)", description = "上传 Excel 进行智能分析 (异步处理)")
    public BaseResponse<Long> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
            ChartGenRequest chartGenRequest) {
        log.info("智能分析 (异步) 请求: {}, 文件: {}", chartGenRequest, multipartFile.getOriginalFilename());
        String name = chartGenRequest.getName();
        String goal = chartGenRequest.getGoal();
        String chartType = chartGenRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 图表类型校验
        if (StringUtils.isNotBlank(chartType)) {
            ThrowUtils.throwIf(ChartTypeEnum.getEnumByText(chartType) == null, ErrorCode.PARAMS_ERROR,
                    "不支持的图表类型：" + chartType);
        }

        // 文件校验
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        ThrowUtils.throwIf(
                originalFilename != null && !originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"),
                ErrorCode.PARAMS_ERROR, "非法文件后缀");

        Long userId = SecurityUtils.getLoginUserId();
        // 限流
        rateLimitUtils.doRateLimit("ai:analysis:" + userId, new TimeModel(1L, TimeUnit.MINUTES), 5L, 1L);

        // 读取 Excel 转为 CSV
        String csvData;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ExcelUtils.excelToCsv(multipartFile.getInputStream(), bos);
            csvData = bos.toString();
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败", e);
        }

        // 先把任务保存到数据库 (状态为 wait)
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus(ChartStatusEnum.WAIT.getValue());
        chart.setUserId(userId);
        chartService.save(chart);

        // 发送到 MQ
        Long chartId = chart.getId();
        ChartAnalysisMessage message = ChartAnalysisMessage.builder()
                .chartId(chartId)
                .build();
        mqSender.send(MqBizTypeEnum.BI_CHART, String.valueOf(chartId), message);

        log.info("智能分析 (异步) 请求发送成功, chartId: {}", chartId);
        return ResultUtils.success(chartId);
    }

    /**
     * 分页获取我的图表列表
     *
     * @param chartQueryRequest 查询请求
     * @return 图表分页
     */
    @PostMapping("/my/list/page/vo")
    @OperationLog(module = "BI 管理", action = "分页获取我的图表列表")
    @Operation(summary = "分页获取我的图表列表")
    public BaseResponse<Page<ChartVO>> listMyChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        log.info("分页获取我的图表列表 请求: {}", chartQueryRequest);
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        chartQueryRequest.setUserId(userId);
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<Chart> queryWrapper = chartService.getQueryWrapper(chartQueryRequest);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size), queryWrapper);
        Page<ChartVO> chartVOPage = chartService.getChartVOPage(chartPage);
        log.info("分页获取我的图表列表 成功, 总数: {}", chartVOPage.getTotal());
        return ResultUtils.success(chartVOPage);
    }

    /**
     * 根据 id 获取图表详情
     *
     * @param id 图表 id
     * @return 图表详情
     */
    @GetMapping("/get/vo")
    @OperationLog(module = "BI 管理", action = "获取图表详情")
    @Operation(summary = "获取图表详情")
    public BaseResponse<ChartVO> getChartVOById(long id) {
        log.info("获取图表详情 请求, id: {}", id);
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Chart chart = chartService.getById(id);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR);
        ChartVO chartVO = chartService.getChartVO(chart);
        log.info("获取图表详情 成功, id: {}", id);
        return ResultUtils.success(chartVO);
    }

    /**
     * 删除图表
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @OperationLog(module = "BI 管理", action = "删除图表")
    @Operation(summary = "删除图表")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest) {
        log.info("删除图表 请求: {}", deleteRequest);
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        if (oldChart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        log.info("删除图表 成功, id: {}", id);
        return ResultUtils.success(b);
    }
}
