package com.qunar.corp.qboss.exam.bank.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qunar.corp.qboss.exam.bank.domain.vo.PageVO;
import com.qunar.corp.qboss.exam.bank.listener.EasyExcelListener;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author chenghaoy.yu
 * @description
 * @email chenghaoy.yu@qunar.com
 * @Date 2024/8/14 9:55
 */
public class EasyExcelUtil extends EasyExcel {

    /**
     * 读取Excel文件
     *
     * @param pathName          文件路径
     * @param head              表头类
     * @param headRowCount      表头行数
     * @param headDataPredicate 表头数据断言
     * @param headDataConsumer  表头数据消费者
     * @param realDataPredicate 真实数据断言
     * @param realDataConsumer  真实数据消费者
     * @param <T>               表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(String pathName, Class<T> head, int headRowCount
            , Predicate<Map<Integer, String>> headDataPredicate, Consumer<List<Map<Integer, String>>> headDataConsumer
            , Predicate<T> realDataPredicate, Consumer<List<T>> realDataConsumer) {
        return read(pathName, head, new EasyExcelListener<>(headRowCount, headDataPredicate, headDataConsumer, realDataPredicate, realDataConsumer));
    }

    /**
     * 无需校验表头和真实数据 读取Excel文件
     *
     * @param pathName         文件路径
     * @param head             表头类
     * @param headRowCount     表头行数
     * @param headDataConsumer 表头数据消费者
     * @param realDataConsumer 真实数据消费者
     * @param <T>              表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(String pathName, Class<T> head, int headRowCount
            , Consumer<List<Map<Integer, String>>> headDataConsumer, Consumer<List<T>> realDataConsumer) {
        return read(pathName, head, new EasyExcelListener<>(headRowCount, (headData) -> true, headDataConsumer, (realData) -> true, realDataConsumer));
    }

    /**
     * 读取Excel文件
     *
     * @param pathName          文件路径
     * @param head              表头类
     * @param headRowCount      表头行数
     * @param batchSize         批处理大小
     * @param headDataPredicate 表头数据断言
     * @param headDataConsumer  表头数据消费者
     * @param realDataPredicate 真实数据断言
     * @param realDataConsumer  真实数据消费者
     * @param <T>               表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(String pathName, Class<T> head, int headRowCount, int batchSize
            , Predicate<Map<Integer, String>> headDataPredicate, Consumer<List<Map<Integer, String>>> headDataConsumer
            , Predicate<T> realDataPredicate, Consumer<List<T>> realDataConsumer) {
        return read(pathName, head, new EasyExcelListener<>(headRowCount, batchSize, headDataPredicate, headDataConsumer, realDataPredicate, realDataConsumer));
    }

    /**
     * 无需校验表头和真实数据 读取Excel文件
     *
     * @param pathName         文件路径
     * @param head             表头类
     * @param headRowCount     表头行数
     * @param batchSize        批处理大小
     * @param headDataConsumer 表头数据消费者
     * @param realDataConsumer 真实数据消费者
     * @param <T>              表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(String pathName, Class<T> head, int headRowCount, int batchSize
            , Consumer<List<Map<Integer, String>>> headDataConsumer, Consumer<List<T>> realDataConsumer) {
        return read(pathName, head, new EasyExcelListener<>(headRowCount, batchSize, (headData) -> true, headDataConsumer, (realData) -> true, realDataConsumer));
    }

    /**
     * 读取Excel文件
     *
     * @param file              文件
     * @param head              表头类
     * @param headRowCount      表头行数
     * @param headDataPredicate 表头数据断言
     * @param headDataConsumer  表头数据消费者
     * @param realDataPredicate 真实数据断言
     * @param realDataConsumer  真实数据消费者
     * @param <T>               表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(File file, Class<T> head, int headRowCount
            , Predicate<Map<Integer, String>> headDataPredicate, Consumer<List<Map<Integer, String>>> headDataConsumer
            , Predicate<T> realDataPredicate, Consumer<List<T>> realDataConsumer) {
        return read(file, head, new EasyExcelListener<>(headRowCount, headDataPredicate, headDataConsumer, realDataPredicate, realDataConsumer));
    }

    /**
     * 无需校验表头和真实数据 读取Excel文件
     *
     * @param file             文件
     * @param head             表头类
     * @param headRowCount     表头行数
     * @param headDataConsumer 表头数据消费者
     * @param realDataConsumer 真实数据消费者
     * @param <T>              表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(File file, Class<T> head, int headRowCount
            , Consumer<List<Map<Integer, String>>> headDataConsumer, Consumer<List<T>> realDataConsumer) {
        return read(file, head, new EasyExcelListener<>(headRowCount, (headData) -> true, headDataConsumer, (realData) -> true, realDataConsumer));
    }

    /**
     * 读取Excel文件
     *
     * @param file              文件
     * @param head              表头类
     * @param headRowCount      表头行数
     * @param batchSize         批处理大小
     * @param headDataPredicate 表头数据断言
     * @param headDataConsumer  表头数据消费者
     * @param realDataPredicate 真实数据断言
     * @param realDataConsumer  真实数据消费者
     * @param <T>               表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(File file, Class<T> head, int headRowCount, int batchSize
            , Predicate<Map<Integer, String>> headDataPredicate, Consumer<List<Map<Integer, String>>> headDataConsumer
            , Predicate<T> realDataPredicate, Consumer<List<T>> realDataConsumer) {
        return read(file, head, new EasyExcelListener<>(headRowCount, batchSize, headDataPredicate, headDataConsumer, realDataPredicate, realDataConsumer));
    }

    /**
     * 无需校验表头和真实数据 读取Excel文件
     *
     * @param file             文件
     * @param head             表头类
     * @param headRowCount     表头行数
     * @param batchSize        批处理大小
     * @param headDataConsumer 表头数据消费者
     * @param realDataConsumer 真实数据消费者
     * @param <T>              表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(File file, Class<T> head, int headRowCount, int batchSize
            , Consumer<List<Map<Integer, String>>> headDataConsumer, Consumer<List<T>> realDataConsumer) {
        return read(file, head, new EasyExcelListener<>(headRowCount, batchSize, (headData) -> true, headDataConsumer, (realData) -> true, realDataConsumer));
    }

    /**
     * 读取Excel文件
     *
     * @param inputStream       输入流
     * @param head              表头类
     * @param headRowCount      表头行数
     * @param headDataPredicate 表头数据断言
     * @param headDataConsumer  表头数据消费者
     * @param realDataPredicate 真实数据断言
     * @param realDataConsumer  真实数据消费者
     * @param <T>               表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(InputStream inputStream, Class<T> head, int headRowCount
            , Predicate<Map<Integer, String>> headDataPredicate, Consumer<List<Map<Integer, String>>> headDataConsumer
            , Predicate<T> realDataPredicate, Consumer<List<T>> realDataConsumer) {
        return read(inputStream, head, new EasyExcelListener<>(headRowCount, headDataPredicate, headDataConsumer, realDataPredicate, realDataConsumer));
    }

    /**
     * 无需校验表头和真实数据 读取Excel文件
     *
     * @param inputStream      输入流
     * @param head             表头类
     * @param headRowCount     表头行数
     * @param headDataConsumer 表头数据消费者
     * @param realDataConsumer 真实数据消费者
     */
    public static <T> ExcelReaderBuilder read(InputStream inputStream, Class<T> head, int headRowCount
            , Consumer<List<Map<Integer, String>>> headDataConsumer, Consumer<List<T>> realDataConsumer) {
        return read(inputStream, head, new EasyExcelListener<>(headRowCount, (headData) -> true, headDataConsumer, (realData) -> true, realDataConsumer));
    }

    /**
     * 读取Excel文件
     *
     * @param inputStream       输入流
     * @param head              表头类
     * @param headRowCount      表头行数
     * @param batchSize         批处理大小
     * @param headDataPredicate 表头数据断言
     * @param headDataConsumer  表头数据消费者
     * @param realDataPredicate 真实数据断言
     * @param realDataConsumer  真实数据消费者
     * @param <T>               表头类
     * @return ExcelReaderBuilder
     */
    public static <T> ExcelReaderBuilder read(InputStream inputStream, Class<T> head, int headRowCount, int batchSize
            , Predicate<Map<Integer, String>> headDataPredicate, Consumer<List<Map<Integer, String>>> headDataConsumer
            , Predicate<T> realDataPredicate, Consumer<List<T>> realDataConsumer) {
        return read(inputStream, head, new EasyExcelListener<>(headRowCount, batchSize, headDataPredicate, headDataConsumer, realDataPredicate, realDataConsumer));
    }

    /**
     * 无需校验表头和真实数据 读取Excel文件
     *
     * @param inputStream      输入流
     * @param head             表头类
     * @param headRowCount     表头行数
     * @param batchSize        批处理大小
     * @param headDataConsumer 表头数据消费者
     * @param realDataConsumer 真实数据消费者
     */
    public static <T> ExcelReaderBuilder read(InputStream inputStream, Class<T> head, int headRowCount, int batchSize,
                                              Consumer<List<Map<Integer, String>>> headDataConsumer, Consumer<List<T>> realDataConsumer) {
        return read(inputStream, head, new EasyExcelListener<>(headRowCount, batchSize, (headData) -> true, headDataConsumer, (realData) -> true, realDataConsumer));
    }

    public static <T> void downloadExcel(HttpServletResponse response, Class<T> head, String fileName, List<T> elements) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            // 这里URLEncoder.encode可以防止中文乱码当fileName为中文
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error while setting up response headers", e);
        }

        EasyExcel.write(response.getOutputStream(), head).sheet().doWrite(elements);
    }

    public static <T, R> void batchDownloadExcel(HttpServletResponse response, Class<T> head, BaseMapper<R> mapper,
                                                 Wrapper<R> wrapper, ExecutorService executorService, String fileName, long pageSize) {
        //1、获取分页总数
        Long total = mapper.selectCount(wrapper);

        //2、计算任务数量
        long taskNum = (total / pageSize) + 1;

        //3、设置响应参数
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            // 这里URLEncoder.encode可以防止中文乱码当fileName为中文
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error while setting up response headers", e);
        }

        //4、根据总页数和分页数量拆分子任务，将任务放入线程池中执行
        for (int i = 1; i <= taskNum; i++) {
            int pageNum = i;
            executorService.submit(() -> {
                //查询出do数据
                Page<R> page = mapper.selectPage(Page.of(pageNum, pageSize), wrapper);
                //转换为T类型的集合
                PageVO<T> tPageVO = PageVO.of(page, head);
                //写入到对应的sheet中
                try {
                    EasyExcel.write(response.getOutputStream(), head).sheet(pageNum).doWrite(tPageVO.getList());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
