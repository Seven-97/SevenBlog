package com.qunar.corp.qboss.exam.bank.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelAnalysisStopException;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ConverterUtils;
import com.qunar.corp.qboss.exam.bank.exception.ExcelValidatorException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author chenghaoy.yu
 * @description 模板方法模式 封装EasyExcel读取过程
 * @email chenghaoy.yu@qunar.com
 * @Date 2024/8/14 9:55
 */
@Slf4j
public class EasyExcelListener<T> implements ReadListener<T> {

    private static final int defaultBatchSize = 5000;

    private final int batchSize;
    private List<Map<Integer, String>> headData;

    private List<T> realData;

    private final int headRowCount;

    private Predicate<Map<Integer, String>> headDataPredicate;

    private final Consumer<List<Map<Integer, String>>> headDataConsumer;

    private Predicate<T> realDataPredicate;

    private final Consumer<List<T>> realDataConsumer;

    private boolean stopRead = false;

    public EasyExcelListener(int headRowCount, Predicate<Map<Integer, String>> headDataPredicate, Consumer<List<Map<Integer, String>>> headDataConsumer
            , Predicate<T> realDataPredicate, Consumer<List<T>> realDataConsumer) {

        this.headRowCount = headRowCount;
        this.batchSize = defaultBatchSize;
        this.headDataPredicate = headDataPredicate;
        this.headDataConsumer = headDataConsumer;
        this.realDataPredicate = realDataPredicate;
        this.realDataConsumer = realDataConsumer;
        headData = new ArrayList<>(headRowCount);
        realData = new ArrayList<>(batchSize);
    }

    public EasyExcelListener(int headRowCount, int batchSize, Predicate<Map<Integer, String>> headDataPredicate, Consumer<List<Map<Integer, String>>> headDataConsumer
            , Predicate<T> realDataPredicate, Consumer<List<T>> realDataConsumer) {

        this.headRowCount = headRowCount;
        this.batchSize = batchSize;
        this.headDataPredicate = headDataPredicate;
        this.headDataConsumer = headDataConsumer;
        this.realDataPredicate = realDataPredicate;
        this.realDataConsumer = realDataConsumer;
        headData = new ArrayList<>(headRowCount);
        realData = new ArrayList<>(batchSize);
    }

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        //获取表头数据
        Map<Integer, String> map = ConverterUtils.convertToStringMap(headMap, context);

        try {
            headDataPredicate.test(map);
        } catch (RuntimeException e) {
            log.error("表头数据不符合要求");
            throw new ExcelValidatorException(e.getMessage());
        }

        headData.add(map);
        if (context.readRowHolder().getRowIndex() == headRowCount - 1) {
            //全部读取完表头数据后，处理表头数据
            headDataConsumer.accept(headData);
        }
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        //判断真实数据是否符合要求
        try {
            realDataPredicate.test(data);
        } catch (RuntimeException e) {
            log.error("数据不符合要求");
            throw new ExcelValidatorException(e.getMessage());
        }

        //获取真实数据
        realData.add(data);
        if (realData.size() >= batchSize) {
            //处理真实数据
            realDataConsumer.accept(realData);
            //清空realData
            realData = new ArrayList<>(batchSize);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 解析完所有excel行, 剩余的数据还需要进行处理
        realDataConsumer.accept(realData);
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        // 如果是某一个单元格的转换异常 能获取到具体行号
        // 如果要获取头的信息 配合invokeHeadMap使用
        if (exception instanceof ExcelDataConvertException e) {
            log.error("第{}行，第{}列解析异常，数据为:{}", e.getRowIndex(),
                    e.getColumnIndex(), e.getCellData());
            //抛出异常后就不会再往下执行了
            throw new ExcelAnalysisStopException("第 %s 行，第 %s 列解析异常".formatted(e.getRowIndex(), e.getColumnIndex()));
        }

        //抛出异常后就不会再往下执行了
        if (exception instanceof ExcelValidatorException) {
            throw new ExcelValidatorException(exception.getMessage());
        }
    }

    @Override
    public boolean hasNext(AnalysisContext context) {
        return !stopRead;
    }
}
