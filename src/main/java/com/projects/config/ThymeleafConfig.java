package com.projects.config;import org.apache.commons.lang3.CharEncoding;import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Configuration;import org.springframework.context.annotation.Description;import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;/** * Created by Adeola.Ojo */@Configurationpublic class ThymeleafConfig {    @Bean    @Description("web page resolver")    public ClassLoaderTemplateResolver classLoaderTemplateResolver() {        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();        resolver.setPrefix("templates");        resolver.setSuffix(".html");        resolver.setTemplateMode("HTML5");        resolver.setCharacterEncoding(CharEncoding.UTF_8);        resolver.setOrder(1);        return resolver;    }    @Bean    @Description("email template resolver")    ClassLoaderTemplateResolver emailTemplateResolver() {        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();        resolver.setPrefix("email");        resolver.setTemplateMode("HTML5");        resolver.setCharacterEncoding(CharEncoding.UTF_8);        resolver.setOrder(1);        return resolver;    }}