package com.seven.mybatisgen;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 在实体中生成 Lombok 注解
 *
 */
public class LombokPlugin extends PluginAdapter {

    public static final DateTimeFormatter DATE_STANDARD_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    public static final String AUTHOR = "mybatis generator";

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    /**
     * 添加头部注释和注解
     */
    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {


        // 添加实体类的 import
        topLevelClass.addImportedType("lombok.Data");
        topLevelClass.addImportedType("lombok.NoArgsConstructor");
        topLevelClass.addImportedType("lombok.AllArgsConstructor");
        topLevelClass.addJavaDocLine("/**");
        topLevelClass.addJavaDocLine(" * <p>");
        topLevelClass.addJavaDocLine(" * " + introspectedTable.getRemarks());
        topLevelClass.addJavaDocLine(" * </p>");
        topLevelClass.addJavaDocLine(" *");
        topLevelClass.addJavaDocLine(" * @author " + AUTHOR);
        topLevelClass.addJavaDocLine(" * @since " + LocalDate.now().format(DATE_STANDARD_FORMAT));
        topLevelClass.addJavaDocLine(" */");
        // 添加实体类的 lombok 注解
        topLevelClass.addAnnotation("@Data");
        topLevelClass.addAnnotation("@NoArgsConstructor");
        topLevelClass.addAnnotation("@AllArgsConstructor");

        return true;
    }

    /**
     * setter生成方法
     */
    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        // 不生成 setter
        return false;
    }

    /**
     * getter生成方法
     */
    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        // 不生成 getter
        return false;
    }
}
