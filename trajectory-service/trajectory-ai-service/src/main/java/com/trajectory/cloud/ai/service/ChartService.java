package com.trajectory.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.trajectory.cloud.ai.model.entity.Chart;
import com.trajectory.cloud.api.ai.model.dto.ChartQueryRequest;
import com.trajectory.cloud.api.ai.model.vo.ChartVO;

/**
 * 图表信息服务
 * <p>
 * 负责智能分析图表的完整生命周期管理，包括图表的创建、查询、转换等核心功能。
 * 核心功能包括：
 * 1. <b>图表生成</b>：支持AI智能分析生成图表，提供同步和异步两种生成模式
 * 2. <b>状态管理</b>：管理图表的执行状态（等待中、运行中、成功、失败）
 * 3. <b>数据转换</b>：将原始数据转换为Echarts可用的图表配置
 * 4. <b>视图转换</b>：提供脱敏的视图对象，保护敏感数据
 * </p>
 *
 * @author StephenQiu30
 */
public interface ChartService extends IService<Chart> {

    /**
     * 根据查询请求构建查询条件
     * <p>
     * 将图表查询请求对象转换为MyBatis Plus的LambdaQueryWrapper，
     * 支持多条件组合查询，如图表名称、类型、状态、用户ID等。
     * </p>
     *
     * @param chartQueryRequest 图表查询请求对象
     * @return MyBatis Plus查询条件封装对象
     */
    LambdaQueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

    /**
     * 获取图表视图对象
     * <p>
     * 将图表实体转换为视图对象，隐藏敏感信息如原始数据等。
     * 根据当前登录用户的权限决定脱敏程度。
     * </p>
     *
     * @param chart 图表实体
     * @return 脱敏后的图表视图对象
     */
    ChartVO getChartVO(Chart chart);

    /**
     * 分页获取图表视图对象
     * <p>
     * 将图表实体分页对象转换为视图对象分页，包含分页元数据和脱敏后的数据。
     * </p>
     *
     * @param chartPage 图表实体分页对象
     * @return 脱敏后的图表视图对象分页
     */
    Page<ChartVO> getChartVOPage(Page<Chart> chartPage);
}
