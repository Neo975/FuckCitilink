package com.hubber;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainCitilink {
    private static final Logger logger = LoggerFactory.getLogger(MainCitilink.class);
    private static String WEBDRIVER_LOCATION = ".//drivers//chromedriver.exe";
    private static int captchaBypassCounter = 0;

    public static void main(String[] args) {
        logger.trace("Method main(), entry point ->");
        setSystemPropertiesForSelenium();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
//        options.addArguments("--disable-gpu");
//        options.addArguments("window-size=800,600");
//        options.addArguments("--disable-dev-shm-usage");

/*
        options.addArguments("start-maximized"); // https://stackoverflow.com/a/26283818/1689770
        options.addArguments("enable-automation"); // https://stackoverflow.com/a/43840128/1689770
        options.addArguments("--headless"); // only if you are ACTUALLY running headless
        options.addArguments("--no-sandbox"); //https://stackoverflow.com/a/50725918/1689770
        options.addArguments("--disable-infobars"); //https://stackoverflow.com/a/43840128/1689770
        options.addArguments("--disable-dev-shm-usage"); //https://stackoverflow.com/a/50725918/1689770
        options.addArguments("--disable-browser-side-navigation"); //https://stackoverflow.com/a/49123152/1689770
        options.addArguments("--disable-gpu"); //https://stackoverflow.com/questions/51959986/how-to-solve-selenium-chromedriver-timed-out-receiving-message-from-renderer-exc
*/

        WebDriver driver = new ChromeDriver(options);
//        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

        test2(driver);

        DBUtils.shutdown();
        driver.close();
        driver.quit();
        logger.trace("Method main(), exit point <-");
    }

    public static void test2(WebDriver driver) {
        logger.trace("Method test2(), entry point ->");
        List<String> listRootURLs = findRootURLs(driver);
        List<String> listFinalURLs = new ArrayList<>();

        for (String rootURL : listRootURLs) {
            findFinalURLS(rootURL, driver);
        }
        logger.trace("Method test2(), exit point <-");
    }

    public static List<String> findRootURLs(WebDriver driver) {
        logger.trace("Method findRootURLs(), entry point ->");
        List<String> res = new ArrayList<>();

        driver.get("https://www.citilink.ru/catalog");
        List<WebElement> list = driver.findElements(By.xpath("//ul[@class='category-catalog__children-list']/li"));
        for (WebElement element : list) {
            String rootURL = element.findElement(By.xpath(".//a")).getAttribute("href");
            res.add(rootURL);
            logger.debug("Method findRootURLs(), root URL found: " + rootURL);
        }

        logger.trace("Method findRootURLs(), exit point <-");
        return res;
    }

//Возвращает список финальных URL для заданного корневого URL.
//В HTML-странице корневого URL могут быть ссылки на дополнительные страницы со списками товаров.
//Цель этого метода - вернуть URL без ссылок на дополнительные страницы со списками товаров.
    public static List<String> findFinalURLS(String rootURL, WebDriver extDriver) {
        logger.trace("Method findFinalURLS(), entry point ->");
        logger.info("Method findFinalURLS(), checking URL: {}", rootURL);
        List<String> res = new ArrayList<>();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
//        options.addArguments("--disable-gpu");
//        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        driver.get(rootURL);

        List<WebElement> list = driver.findElements(By.xpath("//ul[starts-with(@class, 'col subnavigation__left_side_block_ul')]/li"));

        if (list.size() == 0) {
            logger.debug("Method findFinalURLS(), final URL found: {}", rootURL);
            List<ProxyProduct> listProds = parseItems(rootURL, driver);
            DBUtils.save(listProds);
            res.add(rootURL);
            driver.close();
            driver.quit();
            logger.trace("Method findFinalURLS(), listItems.size() == 0, exit point <-");

            return res;
        }

        logger.debug("Method findFinalURLS(), intermediate URL: {}", rootURL);
        for (WebElement element : list) {
            String currentURL = element.findElement(By.xpath(".//a")).getAttribute("href");
            List<String> recursiveList = findFinalURLS(currentURL, driver);
            res.addAll(recursiveList);
        }
        driver.close();
        driver.quit();

        logger.trace("Method findFinalURLS(), exit point <-");
        return res;
    }

//Возвращает список товаров, найденных на странице с указанным URL.
//Метод сам осуществляет пэйджинацию(перелистываение) списка товаров, если список товаров разделен на группы.
    public static List<ProxyProduct> parseItems(String url, WebDriver driver) {
        logger.trace("Method parseItems(), entry point ->");
        List<ProxyProduct> res = new ArrayList<>();
//        driver.get(url);
        checkForCaptcha(url, driver);
        List<WebElement> list = driver.findElements(By.xpath("//div[@class='js--subcategory-product-item subcategory-product-item product_data__gtm-js  product_data__pageevents-js ddl_product']"));

        //Создаем объекты класса com.hubber.Product, забиваем туда description, price и т.д.
        //Все помещаем в список res
        //..
        for (WebElement element : list) {
            String jsonParams = element.getAttribute("data-params");
            ProxyProduct product = new ProxyProduct(jsonParams);
            res.add(product);
        }

        //Осуществляем пэйджинацию, при необходимости
        List<WebElement> pages = driver.findElements(By.xpath("//div[@class='page_listing'][1]//link[@rel='next']"));
        if (pages.size() == 0) {
            logger.trace("Method parseItems(), pages.size() == 0, exit point <-");
            return res;
        }
        res.addAll(parseItems(pages.get(0).getAttribute("href"), driver));

        logger.trace("Method parseItems(), exit point <-");
        return res;
    }

//Проверка на капчу и ее ручной ввод при необходимости
    private static void checkForCaptcha(String url, WebDriver extDriver) {
        logger.trace("Method checkForCaptcha(), entry point ->");
        extDriver.get(url);
        List<WebElement> testList = extDriver.findElements(By.xpath("//div[@class='js--subcategory-product-item subcategory-product-item product_data__gtm-js  product_data__pageevents-js ddl_product']"));

        if (testList != null) {
            captchaBypassCounter = 0;
            logger.debug("Method checkForCaptcha(), captcha bypassed");
            logger.trace("Method checkForCaptcha(), exit point <-");

            return;
        }

        //Требуется ввод капчи
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");
//        options.addArguments("--disable-gpu");
//        options.addArguments("--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        driver.get(url);

        try {
            if (captchaBypassCounter < 3) {
                captchaBypassCounter++;
                Thread.sleep(5000);
                WebElement captchaInput = driver.findElement(By.xpath("//input[@name='captcha']"));
                WebElement captchaButton = driver.findElement(By.xpath("//button[@type='submit']"));
                String captchaValue = "some text";
                captchaInput.sendKeys(captchaValue);
                captchaButton.click();
                logger.debug("Method checkForCaptcha(), captchaBypassCounter < 3");
            } else if (captchaBypassCounter >= 3) {
                captchaBypassCounter++;
                Thread.sleep(100000);
                WebElement captchaInput = driver.findElement(By.xpath("//input[@name='captcha']"));
                WebElement captchaButton = driver.findElement(By.xpath("//button[@type='submit']"));
                String captchaValue = "f";
                captchaInput.sendKeys(captchaValue);
                captchaButton.click();
                logger.debug("Method checkForCaptcha(), captchaBypassCounter >= 3");
            }
        } catch (Exception e) {
            logger.error("Method checkForCaptcha(), exception occurred", e);
            try {
                Thread.sleep(180000);
            } catch (InterruptedException interruptedException) {

            }
        }
        driver.close();
        driver.quit();
        logger.trace("Method checkForCaptcha(), exit point <-");
    }

//Установка свойств системы для ChromeDriver
    private static void setSystemPropertiesForSelenium() {
        logger.trace("Method setSystemPropertiesForSelenium(), entry point ->");
        //Вывод логов Selenium в файл и отключение вывода встандартные потоки вывода и ошибок
        System.setProperty("webdriver.chrome.driver", ".//drivers//chromedriver.exe");
        System.setProperty("webdriver.chrome.logfile", ".//logs//selenium.log");
//        System.setProperty("webdriver.chrome.verboselogging", "false");
//        System.setProperty("webdriver.chrome.silentOutput", "true");
//        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        logger.trace("Method setSystemPropertiesForSelenium(), exit point <-");
    }
}
