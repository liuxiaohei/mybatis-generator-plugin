package com.luca.mybatis.generator.plugins.utils;

import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerClass;

/**
 * 把InnerInterface包装成InnerClass(Mybatis Generator 没有提供内部接口实现)
 */
public class InnerInterfaceWrapperToInnerClass extends InnerClass{
    private InnerInterface innerInterface;  // 内部接口


    public InnerInterfaceWrapperToInnerClass(FullyQualifiedJavaType type) {
        super(type);
    }

    public InnerInterfaceWrapperToInnerClass(String typeName) {
        super(typeName);
    }

    public InnerInterfaceWrapperToInnerClass(InnerInterface innerInterface){
        super(innerInterface.getType());
        this.innerInterface = innerInterface;
    }

    /**
     * 重写获取Java内容方法，调用InnerInterface的实现
     */
    @Override
    public String getFormattedContent(int indentLevel, CompilationUnit compilationUnit) {
        return this.innerInterface.getFormattedContent(indentLevel, compilationUnit);
    }
}
