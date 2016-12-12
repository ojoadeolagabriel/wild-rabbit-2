package com.projects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projects.config.AppConfig;
import com.projects.data.DaoFactory;
import com.projects.data.dao.AutoPayBankProcessorDao;
import com.projects.data.dto.BankProcessor;
import com.projects.util.security.Hash;
import io.advantageous.boon.core.Sys;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.web.Locale;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.client.RestTemplate;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

/**
 * Hello world!
 */
@SpringBootApplication
public class App implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String... args) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            SpringApplication.run(App.class, args);
            System.out.println("houston.. spring initialized successfully");
        } catch (Exception ex) {
            System.out.println("houston.. spring unable to init: " + ex.getMessage());
        }
        startProcessor();
    }

    static int httpRequestCounter = 0;
    static int counter;
    static Vertx vertx;
    static ApplicationContext context;

    /**
     * start Processor
     */
    private static void startProcessor() {
        String data;
        try {
            data = Hash.getMD5Hash("password");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        logger.debug("houston.. booting...");
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true))
                .setWorkerPoolSize(40);

        vertx = Vertx.vertx(vertxOptions);
        MetricsService metricsService = MetricsService.create(vertx);
        WorkerExecutor executor = vertx.createSharedWorkerExecutor("my-worker-pool");
        WorkerExecutor monitorExecutor = vertx.createSharedWorkerExecutor("my-worker-pool");
        WorkerExecutor restTemplateRunnner = vertx.createSharedWorkerExecutor("my-worker-pool");

        vertx.setPeriodic(1000, id -> {
            counter = 0;
            executor.executeBlocking(future -> {
                counter++;
                String result = "..running " + counter;
                future.complete(result);
            }, res -> {
                logger.debug("The result is: " + res.result() + " @ " + new Date());
            });
        });

        AppConfig appConfig = (AppConfig) context.getBean("appConfig");
        RestTemplate pingPongTemplate = (RestTemplate) context.getBean("pingPongTemplate");
        vertx.setPeriodic(2000, id -> {
            restTemplateRunnner.executeBlocking(future -> {
                String response = pingPongTemplate.getForObject(String.format("http://localhost:%s/api/v1/counter", appConfig.hostPort), String.class);
                future.complete(response);
            }, res -> {
                httpRequestCounter++;
                logger.debug("[ " + httpRequestCounter + " ]" + "Rest response data: " + res.result());
            });
        });

        initHttpServer();
    }


    /**
     * init Http Server
     */
    private static void initHttpServer() {
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.get().failureHandler(ctx -> {
            if (ctx.statusCode() == 404) {
                System.out.println("not found oooooooooooo...");
            }
        });

        router.route("/api/v1/greeting").handler(context -> {
            HttpServerResponse response = context.response();
            response.putHeader("content-type", "text/plain");

            context.vertx().setTimer(1, tid -> {
                response.end("hello world from vert.x/web!");
                logger.debug("completed delayed operation...");
            });
        });

        router.route(HttpMethod.GET, "/api/v1/counter").handler(context -> {
            HttpServerResponse response = context.response();
            response.putHeader("content-type", "text/plain");
            for (Locale locale : context.acceptableLocales()) {
                switch (locale.language()) {
                    case "en":
                        response.putHeader("language-found", "english");
                        break;
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            DaoFactory daoFactory = (DaoFactory) App.context.getBean("daoFactory");
            List<BankProcessor> bankProcessors = daoFactory.autoPayBankProcessorDao.fetchAll();
            if (bankProcessors != null) {
                for (BankProcessor processor : bankProcessors) {
                    logger.debug(String.format("found [bank : %s @ %s]", processor.getProcessorDescription(), processor.getCbnCode()));
                }
            }

            BankProcessor processor = daoFactory.autoPayBankProcessorDao.fetchById(4);
            try {
                String returnJson = mapper.writeValueAsString(processor);
                response.end(returnJson);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        });

        AppConfig appConfig = (AppConfig) context.getBean("appConfig");
        httpServer.requestHandler(router::accept).listen(appConfig.hostPort, appConfig.hostIp);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
