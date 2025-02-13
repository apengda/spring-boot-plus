package com.github.pdaodao.springwebplus.base.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.github.pdaodao.springwebplus.base.config.support.*;
import com.github.pdaodao.springwebplus.base.frame.AppCustomConfig;
import com.github.pdaodao.springwebplus.base.support.ProxyServlet;
import com.github.pdaodao.springwebplus.tool.util.Preconditions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.*;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
@AllArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final SysConfigProperties configProperties;
    private final ProxyServlet proxyServlet;
    private final Optional<AppCustomConfig> sysDateTimeFormatOption;
    private final List<InterceptorRegistryListener> interceptorRegistryListeners;



    @Bean
    @ConditionalOnProperty("http.proxy")
    public ServletRegistrationBean proxyServletRegistrationBean() {
        final List<String> list = StrUtil.split(configProperties.getHttpProxy(), ",");
        for (final String p : list) {
            final List<String> part = StrUtil.split(p, "->");
            Preconditions.checkArgument(part.size() == 2, "error http-proxy mapping config:" + p);
            proxyServlet.addMapping(part.get(0).trim(), part.get(1).trim());
        }
        final ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(proxyServlet, proxyServlet.urlMapping());
        log.warn("proxy-servlet:" + StrUtil.join(", ", proxyServlet.urlMapping()));
        return servletRegistrationBean;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            final String dateFormat = sysDateTimeFormatOption.isPresent() ? sysDateTimeFormatOption.get().dateFormat() : "yyyy-MM-dd";
            final String dateTimeFormat = sysDateTimeFormatOption.isPresent() ? sysDateTimeFormatOption.get().datetimeFormat() : "yyyy-MM-dd HH:mm:ss";
            builder.simpleDateFormat(dateTimeFormat);
            builder.failOnUnknownProperties(false);
            builder.timeZone("Asia/Shanghai");
            builder.featuresToEnable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(dateFormat)));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(dateFormat)));
        };
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (StringUtils.isNotBlank(configProperties.getCorsPath()) && StrUtil.isNotBlank(configProperties.getCorsOrigins())) {
            for (final String p : configProperties.getCorsPath().split(",")) {
                registry.addMapping(p)
                        .allowedOriginPatterns(configProperties.getCorsOrigins().split(","))
                        .allowedMethods("POST", "GET", "PUT", "OPTIONS")
                        .allowedHeaders(configProperties.getCorsHead().split(","))
                        .maxAge(3600)
                        .allowCredentials(true);
            }
        }
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("xml", MediaType.APPLICATION_XML);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PageRequestParamResolver());
        resolvers.add(new CurrentUserInfoParamResolver());
    }


    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new HolderClearInterceptor());
        if (CollUtil.isNotEmpty(interceptorRegistryListeners)) {
            for (InterceptorRegistryListener listener : interceptorRegistryListeners) {
                listener.addInterceptors(registry);
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = "file:" + configProperties.getStaticPath().trim();
        if (!path.endsWith("/")) {
            path += "/";
        }
        fileResourceMap(registry, "", path);
        // 处理子应用
        webappSubPathProcess(path, registry);
    }

    /**
     * 处理前端子应用的情况
     *
     * @param rootPath
     * @param registry
     */
    protected void webappSubPathProcess(final String rootPath, final ResourceHandlerRegistry registry) {
        if (!FileUtil.exist(configProperties.getStaticPath())) {
            return;
        }
        final List<File> fs = FileUtil.loopFiles(FileUtil.newFile(configProperties.getStaticPath()), 1, null);
        if (CollUtil.isEmpty(fs)) {
            return;
        }
        for (final File f : fs) {
            if (f.isDirectory() && !WebappFile.FileNames.contains(f.getName())) {
                final String subAppName = f.getName();
                log.info("add sub  app:{}", subAppName);
                WebappFile.SubApps.add(subAppName);
                final String filePath = rootPath + subAppName + "/";
                fileResourceMap(registry, subAppName, filePath);
            }
        }
    }

    /**
     * 静态资源请求映射
     *
     * @param registry
     * @param app
     * @param filePath
     */
    private void fileResourceMap(final ResourceHandlerRegistry registry, final String app, final String filePath) {
        registry.addResourceHandler(app + "/index.html")
                .addResourceLocations(filePath + "index.html")
                .setCacheControl(CacheControl.noStore());

        registry.addResourceHandler(app)
                .addResourceLocations(filePath + "index.html")
                .setCacheControl(CacheControl.noStore());

        registry.addResourceHandler(app + "/favicon.ico")
                .addResourceLocations(filePath + "favicon.ico")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));

        registry.addResourceHandler(app + "/assets/**")
                .addResourceLocations(filePath + "assets/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));

        registry.addResourceHandler(app + "/static/**")
                .addResourceLocations(filePath + "static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(1)));

        registry.addResourceHandler(app + "/public/**")
                .addResourceLocations(filePath + "public/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(1)));
    }
}